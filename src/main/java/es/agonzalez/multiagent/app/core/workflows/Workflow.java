package es.agonzalez.multiagent.app.core.workflows;

public interface Workflow<I, O> {
    O run(I input);
}
