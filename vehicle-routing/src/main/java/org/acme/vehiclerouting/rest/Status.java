package org.acme.vehiclerouting.rest;

import java.util.Set;

import org.acme.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.core.api.solver.SolverStatus;

class Status {

    public final VehicleRoutingSolution solution;
    public final String scoreExplanation;
    public final boolean isSolving;
    public final long idBestSolution;

    Status(VehicleRoutingSolution solution, String scoreExplanation, Set<SolverStatus> solverStates,
            long solutionId) {
        this.solution = solution;
        this.scoreExplanation = scoreExplanation;
        this.isSolving = !solverStates.equals(Set.of(SolverStatus.NOT_SOLVING));
        this.idBestSolution = solutionId;
    }
}
