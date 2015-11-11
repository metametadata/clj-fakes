package interop;

public interface InterfaceFixture {
    int overloadedMethodWithDifferentReturnTypes(char c);
    int overloadedMethodWithDifferentReturnTypes(int c);
    int overloadedMethodWithDifferentReturnTypes(double c);
    String overloadedMethodWithDifferentReturnTypes(String s);
    String overloadedMethodWithDifferentReturnTypes();
    String overloadedMethodWithDifferentReturnTypes(boolean b);
    boolean overloadedMethodWithDifferentReturnTypes(int x, int y);
}
