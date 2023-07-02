package org.acme.vehiclerouting.rest;

import java.util.Set;

import org.acme.vehiclerouting.domain.VehicleRoutingSolution;
import org.acme.vehiclerouting.persistence.Solution;
import org.optaplanner.core.api.solver.SolverStatus;

class Status {

    public final VehicleRoutingSolution solution;
    public final String scoreExplanation;
    public final boolean isSolving;
    public final long idBestSolution;
    public final int solutionIteration;
    public final int currentIteration;

    Status(Solution solution, String scoreExplanation, Set<SolverStatus> solverStates, int currentIteration) {
        this.solution = solution.getVehicleRoutingSolution();
        this.scoreExplanation = scoreExplanation;
        this.isSolving = !solverStates.equals(Set.of(SolverStatus.NOT_SOLVING));
        this.idBestSolution = solution.getId();
        this.solutionIteration = solution.getLastUpdate();
        this.currentIteration = currentIteration;
    }
}
