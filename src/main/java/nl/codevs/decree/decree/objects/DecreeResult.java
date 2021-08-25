package nl.codevs.decree.decree.objects;

public enum DecreeResult {
    SUCCESSFUL,
    NO_ARGUMENTS,
    NOT_FOUND;

    public boolean isFailed(){
        return this.equals(NOT_FOUND);
    }
}
