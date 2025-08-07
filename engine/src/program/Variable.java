package program;

class Variable {
    String name;
    int value;

    Variable(String name, int value) {
        this.name = name;
        this.value = value;
    }

    String getName() {
        return name;
    }
    int getValue() {
        return value;
    }
    void setValue(int value) {
        this.value = value;
    }
}
