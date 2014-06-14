package io.llambda.compiler.codegen
import io.llambda

import scala.io.Source

import llambda.compiler._
import llambda.llvmir._
import llambda.compiler.platform.TargetPlatform
import llambda.compiler.planner.{step => ps}
import llambda.compiler.{celltype => ct}

object GenProgram {
  def resourceAsString(resourcePath : String) : String = {
    val stream = getClass.getClassLoader.getResourceAsStream(resourcePath)
    Source.fromInputStream(stream).mkString
  }

  def preludeIr : String = {
    List(
      resourceAsString("generated/cellTypes.ll"),
      resourceAsString("defines.ll")
    ) mkString "\n"
  }

  def apply(functions : Map[String, planner.PlannedFunction], compileConfig : CompileConfig, featureIdentifiers : Set[String], entryFilenameOpt : Option[String]) : String = {
    val module = new IrModuleBuilder
    module.metadataIndexSource.nextIndex = ct.CellType.nextMetadataIndex

    // Identify ourselves in our generated IR
    val compilerIdentifier = FeatureIdentifiers.compilerVersionIdentifier + " (based on LLVM)"
    module.identifyCompiler(compilerIdentifier)

    val debugInfoGeneratorOpt = if (compileConfig.genDebugInfo) {
      Some(new DebugInfoGenerator(module, compileConfig, compilerIdentifier, entryFilenameOpt))
    }
    else {
      None
    }

    val plannedSymbols = functions.keySet
    val typeGenerator = new TypeGenerator(module, compileConfig.targetPlatform)

    // Package up our global generator state
    val genGlobals = GenGlobals(
      plannedSymbols=plannedSymbols,
      typeGenerator=typeGenerator,
      debugInfoGeneratorOpt=debugInfoGeneratorOpt,
      targetPlatform=compileConfig.targetPlatform
    )

    // Build each program-supplied function
    val functionGenerator = GenFunction(module, genGlobals)_ 

    for((nativeSymbol, plannedFunction) <- functions) {
      functionGenerator(nativeSymbol, plannedFunction)
    }
    
    // Build our main() glue to init the runtime and call our program
    val result = IrFunction.Result(IntegerType(32))
    val namedArguments = List(
      "argc" -> IrFunction.Argument(IntegerType(32)),
      "argv" -> IrFunction.Argument(PointerType(PointerType(IntegerType(8))))
    )

    val mainFunction = new IrFunctionBuilder(
      module=module,
      result=result,
      namedArguments=namedArguments,
      name="main"
    )

    val entryBlock = mainFunction.entryBlock

    // Initialize our runtime
    val initDecl = RuntimeFunctions.init
    val launchWorldDecl = RuntimeFunctions.launchWorld

    module.declareFunction(initDecl) 
    module.declareFunction(launchWorldDecl)
    entryBlock.callDecl(None)(initDecl, Nil)
    
    // Call __llambda_top_level through _lliby_launch_world
    // __llambda_top_level must be defined by the planner
    val execValue = GenNamedEntryPoint(module)(LlambdaTopLevelSignature, LlambdaTopLevelSignature.nativeSymbol, plannedSymbols) 

    entryBlock.callDecl(None)(launchWorldDecl, List(execValue), false)

    // Return 0
    // Scheme can only return non-zero exit codes using (exit)
    entryBlock.ret(IntegerConstant(IntegerType(32), 0))

    module.defineFunction(mainFunction)

    // Dump our type map
    typeGenerator.emitTypeMaps()

    // Generate any final debug info
    for(debugInfoGenerator <- debugInfoGeneratorOpt) {
      debugInfoGenerator.finish()
    }

    // Convert our IR to one big string
    preludeIr + "\n" + module.toIr + "\n"
  }
}

