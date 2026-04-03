package family.balling.descendencytracker;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public final class TestMain {
    private TestMain() {
    }

    public static void main(String[] args) throws Exception {
        List<Class<?>> testClasses = List.of(
                family.balling.descendencytracker.persistence.SchemaMigratorTest.class,
                family.balling.descendencytracker.application.PersonCsvServiceTest.class
        );

        int failures = 0;
        for (Class<?> testClass : testClasses) {
            Object instance = testClass.getDeclaredConstructor().newInstance();
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.getAnnotation(Test.class) == null) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                    System.out.println("PASS " + testClass.getSimpleName() + "." + method.getName());
                } catch (InvocationTargetException ex) {
                    failures++;
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    System.err.println("FAIL " + testClass.getSimpleName() + "." + method.getName() + ": " + cause.getMessage());
                    cause.printStackTrace(System.err);
                }
            }
        }

        if (failures > 0) {
            throw new IllegalStateException("Test failures: " + failures);
        }
    }
}
