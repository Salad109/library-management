package librarymanagement;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import librarymanagement.copies.CopyRepository;
import librarymanagement.copies.CopyService;
import librarymanagement.utils.IntegrationTest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "librarymanagement")
class ArchitectureTest {

    // Jackson 2 is on the classpath only for springdoc
    @ArchTest
    static final ArchRule noJackson2 = noClasses()
            .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson.databind..");

    @ArchTest
    static final ArchRule controllersDontUseEntities = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    // Without it the spec documents the Pageable as one object instead of page/size/sort query params
    @ArchTest
    static final ArchRule pageablesAreParameterObjects = noMethods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .should(ArchCondition.from(DescribedPredicate.describe(
                    "take a Pageable without @ParameterObject",
                    ArchitectureTest::hasBarePageable)));

    // Validation lives on the request records
    @ArchTest
    static final ArchRule entitiesHaveNoValidation = noClasses()
            .that().areAnnotatedWith(Entity.class)
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..");

    // Flyway owns the schema, so nothing reads these
    @ArchTest
    static final ArchRule entitiesHaveNoDdlAttributes = noFields()
            .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
            .should().beAnnotatedWith(DescribedPredicate.describe(
                    "@Column(length/nullable/unique) or @JoinColumn(nullable)",
                    ArchitectureTest::isDdlOnly));

    // CopyService.save() evicts the book's cached availableCopies
    @ArchTest
    static final ArchRule copyWritesGoThroughSave = noClasses()
            .should().callMethodWhere(DescribedPredicate.describe(
                    "a CopyRepository save or delete outside CopyService.save()",
                    ArchitectureTest::isCopyWriteOutsideSave));

    // Each request commits like in production
    @ArchTest
    static final ArchRule integrationTestClassesArentTransactional = noClasses()
            .that().areAssignableTo(IntegrationTest.class)
            .should().beAnnotatedWith(Transactional.class)
            .orShould().beAnnotatedWith("jakarta.transaction.Transactional");

    @ArchTest
    static final ArchRule integrationTestMethodsArentTransactional = noMethods()
            .that().areDeclaredInClassesThat().areAssignableTo(IntegrationTest.class)
            .should().beAnnotatedWith(Transactional.class)
            .orShould().beAnnotatedWith("jakarta.transaction.Transactional");

    private static boolean hasBarePageable(JavaMethod method) {
        for (JavaParameter parameter : method.getParameters()) {
            if (parameter.getRawType().isAssignableTo(Pageable.class)
                    && !parameter.isAnnotatedWith(ParameterObject.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDdlOnly(JavaAnnotation<?> annotation) {
        if (annotation.getRawType().isEquivalentTo(Column.class)) {
            return annotation.hasExplicitlyDeclaredProperty("length")
                    || annotation.hasExplicitlyDeclaredProperty("nullable")
                    || annotation.hasExplicitlyDeclaredProperty("unique");
        }
        if (annotation.getRawType().isEquivalentTo(JoinColumn.class)) {
            return annotation.hasExplicitlyDeclaredProperty("nullable");
        }
        return false;
    }

    private static boolean isCopyWriteOutsideSave(JavaMethodCall call) {
        boolean copyWrite = call.getTargetOwner().isAssignableTo(CopyRepository.class)
                && (call.getName().startsWith("save") || call.getName().startsWith("delete"));
        boolean fromSave = call.getOrigin().getOwner().isEquivalentTo(CopyService.class)
                && call.getOrigin().getName().equals("save");
        return copyWrite && !fromSave;
    }
}
