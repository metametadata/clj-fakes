package interop;

public interface InterfaceFixture {
    int overloadedMethodWithDifferentReturnTypes(char x);
    int overloadedMethodWithDifferentReturnTypes(int x);
    int overloadedMethodWithDifferentReturnTypes(double x);
    String overloadedMethodWithDifferentReturnTypes(String x);
    String overloadedMethodWithDifferentReturnTypes();
    String overloadedMethodWithDifferentReturnTypes(boolean x);
    boolean overloadedMethodWithDifferentReturnTypes(int x, int y);

    // With array arg
    String overloadedMethodWithDifferentReturnTypes(int[] x);
    String overloadedMethodWithDifferentReturnTypes(Integer[] x);

    // With array return value
    int[] overloadedMethodWithDifferentReturnTypes(int x, int y, int z);
    Integer[] overloadedMethodWithDifferentReturnTypes(double x, int y);

    // With varargs and arrays
    String overloadedMethodWithDifferentReturnTypes(int x, Integer... args);
    String overloadedMethodWithDifferentReturnTypes(Double... args);

    // Varargs (without overloading)
    String methodWithVarargs(int x, String... args);
}