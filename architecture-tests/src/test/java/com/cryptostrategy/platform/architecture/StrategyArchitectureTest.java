package com.cryptostrategy.platform.architecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
class StrategyArchitectureTest {
    @Test void strategyContractsAreTechnologyPure(){var classes=new ClassFileImporter().importPackages("com.cryptostrategy.platform.strategy.api");noClasses().should().dependOnClassesThat().resideInAnyPackage("org.springframework..","java.sql..","javax.sql..","com.cryptostrategy.platform.persistence..","com.cryptostrategy.platform.marketdata.internal..").check(classes);}
    @Test void pluginModulesDoNotReachStrategyInternals(){var classes=new ClassFileImporter().importPackages("com.cryptostrategy.platform.strategies","com.cryptostrategy.platform.combination");noClasses().should().dependOnClassesThat().resideInAPackage("com.cryptostrategy.platform.strategy.internal..").check(classes);}
}
