package io.llambda.compiler.reducer
import io.llambda

import scala.collection.breakOut

import llambda.compiler._
import llambda.compiler.reducer.{partialvalue => pv}

private[reducer] object ReduceExpr {
  private def unflattenExprs(exprs : List[et.Expr])(implicit reduceConfig : ReduceConfig) : et.Expr = exprs match {
    case Nil =>
      et.Begin(Nil)

    case nonEmptyExprs =>
      // Get rid of any pure expressions that aren't in the last position
      val nonValueExprs = nonEmptyExprs.dropRight(1)
      val valueExpr = nonEmptyExprs.last

      val newExprs = nonValueExprs.filter(ExprHasSideEffects) :+ valueExpr
      
      et.Expr.fromSequence(newExprs)
  }

  /** Reduces an expression to a simpler form while preserving meaning
    *
    * This works by recursively performing partial evaluation on expressions in an attempt to reduce the amount of
    * work that needs to be done at runtime.
    */
  def apply(expr : et.Expr)(implicit reduceConfig : ReduceConfig) : et.Expr = (expr match {
    case begin : et.Begin =>
      val mappedExprs = begin.toSequence.map(apply)
      unflattenExprs(mappedExprs)

    case et.Apply(appliedExpr, operands) =>
      val reducedOperands = operands.map(ReduceExpr(_))

      ReduceApplication(expr, appliedExpr, reducedOperands) getOrElse {
        // We declined reducing this application
        et.Apply(apply(appliedExpr), reducedOperands)  
      }

    case et.TopLevelDefinition(bindings) =>
      // Reduce our bindings and drop unused pure bindings
      val usedBindings = (bindings.map { case (storageLoc, initializer) =>
        storageLoc -> ReduceExpr(initializer)
      }).filter { case (storageLoc, reducedInitializer) =>
        TopLevelDefinitionRequired(storageLoc, reducedInitializer, reduceConfig.analysis)
      }

      usedBindings match {
        case Nil =>
          et.Literal(ast.UnitValue())

        case nonEmptyBindings =>
          et.TopLevelDefinition(nonEmptyBindings)
      }

    case et.InternalDefinition(bindings, bodyExpr) =>
      // Just reduce our bindings first
      val reducedBindings = bindings.map { case (storageLoc, initializer) =>
        storageLoc -> ReduceExpr(initializer)
      }

      // Now convert them to partial values
      val bindingPartialValues = reducedBindings.map({ case(storageLoc, reducedInitializer) =>
        storageLoc -> pv.PartialValue.fromReducedExpr(reducedInitializer)
      })(breakOut) : Map[StorageLocation, pv.PartialValue]
      
      // We can use any non-mutable partial values as known values for the body
      val newKnownValues = bindingPartialValues.filter({ case (storageLoc, _) =>
        !reduceConfig.analysis.mutableVars.contains(storageLoc)
      })
      
      val bodyConfig = reduceConfig.copy(
        knownValues=(reduceConfig.knownValues ++ newKnownValues)
      )

      // Reduce the body
      val reducedBody = ReduceExpr(bodyExpr)(bodyConfig)

      // Strip unused bindings
      // Note that unlike lambdas we need to handle recursively defined values in our bindings
      val bodyUsedVars = ReferencedVariables(reducedBody)    
      val recursiveBindingUsedVars = (reducedBindings.flatMap { case (_, initializer) =>
        ReferencedVariables(initializer)
      }).toSet

      val allUsedVars = bodyUsedVars ++ recursiveBindingUsedVars 

      val usedBindings = reducedBindings.filter { case (storageLoc, initializer) =>
        allUsedVars.contains(storageLoc) ||
          reduceConfig.analysis.mutableVars.contains(storageLoc) ||
          ExprHasSideEffects(initializer) ||
          !PartialValueSatisfiesType(bindingPartialValues(storageLoc), storageLoc.schemeType)
      }
       
      if (usedBindings.isEmpty) {
        // We can drop the InternalDefinition entirely and use the unwrapped body
        reducedBody
      }
      else {
        et.InternalDefinition(usedBindings, reducedBody)
      }

    case et.Cond(testExpr, trueExpr, falseExpr) =>
      val reducedTest = apply(testExpr)
      val testHasEffects = ExprHasSideEffects(reducedTest)
      
      // Try to optimize out the branch
      // We specially handle impure tests below
      LiteralForExpr(reducedTest, allowImpureExprs=true) match {
        case Some(literal) =>
          val branchExpr = if (literal == ast.BooleanLiteral(false)) {
            apply(falseExpr)
          }
          else {
            apply(trueExpr)
          }

          if (testHasEffects) {
            // Need the test and the branch expression
            unflattenExprs(
              reducedTest.toSequence ++ branchExpr.toSequence
            )
          }
          else {
            // Just need the branch expression
            branchExpr
          }

        case _ =>
          // Can't statically evaluate the test
          et.Cond(
            reducedTest,
            apply(trueExpr),
            apply(falseExpr)
          )
      }

    case unknownExpr =>
      // Unknown expression - map subexpressions
      unknownExpr.map(apply)
  }).assignLocationFrom(expr)
}
