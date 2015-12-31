package mods.eln.server;

//Console argument container (with valid flag)
public class ConsoleArg<Type> {
    public boolean valid;
    public Type value;

    public ConsoleArg(boolean valid, Type value){
        this.valid = valid;
        this.value = value;
    }
}

