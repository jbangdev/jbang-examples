package domain;

public class Hello {
    private static final String TEMPLATE = "Hello, %s!";

    public String name;

    public Hello(String name) {
        this.name = String.format(TEMPLATE, name);
    }
}
