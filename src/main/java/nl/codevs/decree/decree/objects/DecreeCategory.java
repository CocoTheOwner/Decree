package nl.codevs.decree.decree.objects;

public class DecreeCategory implements Decreed {
    public final DecreeCategory parent;
    public final Decree decree;

    public DecreeCategory(DecreeCategory parent, Decree decree) {
        this.parent = parent;
        this.decree = decree;
    }

    @Override
    public Decreed parent() {
        return parent;
    }

    @Override
    public Decree decree() {
        return decree;
    }


}
