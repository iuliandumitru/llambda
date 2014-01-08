package io.llambda.compiler.planner
import io.llambda

import collection.mutable

import llambda.compiler.{et, StorageLocation, ValueNotApplicableException, ReportProcedure, SourceLocated}
import llambda.compiler.{celltype => ct}
import llambda.compiler.{valuetype => vt}
import llambda.compiler.planner.{step => ps}
import llambda.compiler.planner.{intermediatevalue => iv}

private[planner] object PlanExpression {
  // These objects know how to implement certain report procedure directly
  // with plan steps
  private val reportProcPlanners = List(
    reportproc.CadrProcPlanner
  )

  private def lazyPlannedFunction(suggestedName : String, plannedFunction : PlannedFunction)(implicit plan : PlanWriter) : iv.IntermediateValue = {
    // Don't emit this function until its referenced
    val symbolBlock = () => {
      val nativeSymbol = plan.allocProcedureSymbol(suggestedName)
      plan.plannedFunctions += (nativeSymbol -> plannedFunction)

      nativeSymbol
    }

    new iv.KnownProcedure(plannedFunction.signature, symbolBlock, None)
  }

  def apply(initialState : PlannerState)(expr : et.Expression, sourceNameHint : Option[String] = None)(implicit planConfig : PlanConfig, plan : PlanWriter) : PlanResult = LocateExceptionsWith(expr) {
    expr match {
      case et.Begin(exprs) =>
        var finalValue : iv.IntermediateValue = iv.UnitValue

        val finalState = exprs.foldLeft(initialState) { case (state, expr) =>
          val result = apply(state)(expr)
          finalValue = result.value

          result.state
        }

        PlanResult(state=finalState, value=finalValue)

      case et.Apply(procExpr, operandExprs) =>
        val procResult = apply(initialState)(procExpr)

        val operandBuffer = new mutable.ListBuffer[(SourceLocated, iv.IntermediateValue)]

        val finalState = operandExprs.foldLeft(procResult.state) { case (state, operandExpr) =>
          val operandResult = apply(state)(operandExpr)

          operandBuffer += ((operandExpr, operandResult.value))
          operandResult.state
        }

        val operands = operandBuffer.toList

        val invokableProc = procResult.value.toInvokableProcedure() getOrElse {
          throw new ValueNotApplicableException(expr)
        }

        procResult.value match {
          case knownProc : iv.KnownProcedure if knownProc.reportName.isDefined && planConfig.optimize =>
            val reportName = knownProc.reportName.get

            // Give our reportProcPlanners a chance to plan this more
            // efficiently than a function call
            for(reportProcPlanner <- reportProcPlanners) {
              for(planResult <- reportProcPlanner(finalState)(reportName, operands)) {
                // We created an alternative plan; we're done
                return planResult
              }
            }
          
          case other => other
        }

        // Perform a function call
        val applyValueOpt = PlanApplication(invokableProc, operands) 

        PlanResult(
          state=finalState,
          value=applyValueOpt.getOrElse(iv.UnitValue))

      case et.Bind(bindings) =>
        val finalState = bindings.foldLeft(initialState) { case (state, (storageLoc, initialValue)) =>
          if (planConfig.analysis.mutableVars.contains(storageLoc)) {
            val allocTemp = new ps.TempAllocation
            val mutableTemp = new ps.TempValue
            val variableTemp = new ps.TempValue
            
            val initialValueResult = apply(state)(initialValue)
            val initialValueTemp = initialValueResult.value.toTempValue(vt.IntrinsicCellType(ct.DatumCell))

            plan.steps += ps.AllocateCells(allocTemp, 1)

            // Create a new mutable
            val recordDataTemp = new ps.TempValue
            plan.steps += ps.RecordLikeInit(mutableTemp, recordDataTemp, allocTemp, 0, vt.MutableType)

            // Set the value
            plan.steps += ps.RecordDataFieldSet(recordDataTemp, vt.MutableType, vt.MutableField, initialValueTemp)
            
            initialValueResult.state.withMutable(storageLoc -> mutableTemp)
          }
          else {
            // Send a hint about our name
            val initialValueResult = apply(state)(initialValue, Some(storageLoc.sourceName))

            val reportNamedValue = (initialValueResult.value, storageLoc) match {
              case (knownProc : iv.KnownProcedure, reportProc : ReportProcedure) =>
                // Annotate with our report name so we can optimize when we try
                // to apply this
                // Note this is agnostic to if the implementation is a native
                // function versus a Scheme procedure
                knownProc.withReportName(reportProc.reportName)

              case (otherValue, _) =>
                otherValue
            }

            // No planning, just remember this intermediate value
            initialValueResult.state.withImmutable(storageLoc -> reportNamedValue)
          }
        }

        PlanResult(
          state=finalState,
          value=iv.UnitValue
        )

      case et.VarRef(storageLoc : StorageLocation) if !planConfig.analysis.mutableVars.contains(storageLoc) =>
        // Return the value directly 
        PlanResult(
          state=initialState,
          value=initialState.immutables(storageLoc)
        )
      
      case et.VarRef(storageLoc) =>
        val mutableTemp = initialState.mutables(storageLoc)
        
        // Load our data pointer
        val recordDataTemp = new ps.TempValue
        plan.steps += ps.StoreRecordLikeData(recordDataTemp, mutableTemp, vt.MutableType)
        
        // Load the data
        val resultTemp = new ps.TempValue
        plan.steps += ps.RecordDataFieldRef(resultTemp, recordDataTemp, vt.MutableType, vt.MutableField)
        
        // We can be anything here
        val possibleTypes = ct.DatumCell.concreteTypes

        PlanResult(
          state=initialState,
          value=new iv.IntrinsicCellValue(possibleTypes, ct.DatumCell, resultTemp)
        )
      
      case et.MutateVar(storageLoc, valueExpr) =>
        val mutableTemp = initialState.mutables(storageLoc)
        
        val newValueResult = apply(initialState)(valueExpr)
        val newValueTemp = newValueResult.value.toTempValue(vt.IntrinsicCellType(ct.DatumCell))

        // Load our data pointer
        val recordDataTemp = new ps.TempValue
        plan.steps += ps.StoreRecordLikeData(recordDataTemp, mutableTemp, vt.MutableType)
        
        // Store the data
        val resultTemp = new ps.TempValue
        plan.steps += ps.RecordDataFieldSet(recordDataTemp, vt.MutableType, vt.MutableField, newValueTemp)

        PlanResult(
          state=newValueResult.state,
          value=iv.UnitValue
        )

      case et.Literal(value) =>
        PlanResult(
          state=initialState,
          value=DatumToConstantValue(value)
        )

      case et.Cond(testExpr, trueExpr, falseExpr) =>
        val testResult = apply(initialState)(testExpr)
        val truthyPred = testResult.value.toTruthyPredicate()

        val trueWriter = plan.forkPlan()
        val trueValue = apply(testResult.state)(trueExpr)(planConfig, trueWriter).value

        val falseWriter = plan.forkPlan() 
        val falseValue = apply(testResult.state)(falseExpr)(planConfig, falseWriter).value
    
        val planPhiResult = trueValue.planPhiWith(falseValue)(trueWriter, falseWriter)

        plan.steps += ps.CondBranch(
          planPhiResult.resultTemp,
          truthyPred,
          trueWriter.steps.toList, planPhiResult.ourTempValue,
          falseWriter.steps.toList, planPhiResult.theirTempValue
        )

        PlanResult(
          state=testResult.state,
          value=planPhiResult.resultIntermediate
        )
      
      case nativeFunc : et.NativeFunction =>
        PlanResult(
          state=initialState,
          value=new iv.KnownProcedure(nativeFunc, () => nativeFunc.nativeSymbol, None)
        )

      case recordConstructor : et.RecordTypeConstructor =>
        val procName = sourceNameHint.getOrElse {
          // By convention the constructor name is the type name without <>
          recordConstructor.recordType.sourceName
            .replaceAllLiterally("<", "")
            .replaceAllLiterally(">", "")
        }

        PlanResult(
          state=initialState,
          value=lazyPlannedFunction(
            procName,
            PlanRecordTypeConstructor(recordConstructor)
          )
        )
      
      case recordPredicate : et.RecordTypePredicate =>
        val procName = sourceNameHint.getOrElse {
          // By convention the predicate name is the type name without <> followed by ?
          recordPredicate.recordType.sourceName
            .replaceAllLiterally("<", "")
            .replaceAllLiterally(">", "") + "?"
        }

        PlanResult(
          state=initialState,
          value=lazyPlannedFunction(
            procName,
            PlanRecordTypePredicate(recordPredicate)
          )
        )
      
      case recordAccessor : et.RecordTypeAccessor =>
        val procName = sourceNameHint.getOrElse {
          // By convention the accessor name is "{constructorName}-{fieldName}"
          recordAccessor.recordType.sourceName
            .replaceAllLiterally("<", "")
            .replaceAllLiterally(">", "") + 
            "-" + recordAccessor.field.sourceName
        }

        PlanResult(
          state=initialState,
          value=lazyPlannedFunction(
            procName, 
            PlanRecordTypeAccessor(recordAccessor)
          )
        )
      
      case recordMutator : et.RecordTypeMutator =>
        val procName = sourceNameHint.getOrElse {
          // By convention the mutatorName name is "set-{constructorName}-{fieldName}!"
          "set-" +
            recordMutator.recordType.sourceName
            .replaceAllLiterally("<", "")
            .replaceAllLiterally(">", "") + 
            "-" + recordMutator.field.sourceName +
            "!"
        }

        PlanResult(
          state=initialState,
          value=lazyPlannedFunction(
            procName,
            PlanRecordTypeMutator(recordMutator)
          )
        )

      case et.Cast(valueExpr, targetType) =>
        val valueResult = apply(initialState)(valueExpr)

        val castTemp = valueResult.value.toTempValue(targetType)
        val castValue = TempValueToIntermediate(targetType, castTemp)
          
        PlanResult(state=valueResult.state, value=castValue)

      case et.Lambda(fixedArgs, restArg, body) =>
        PlanLambda(initialState, plan)(fixedArgs, restArg, body, sourceNameHint)
    }  
  }
}
