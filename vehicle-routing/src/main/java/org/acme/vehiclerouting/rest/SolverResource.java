package org.acme.vehiclerouting.rest;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.acme.vehiclerouting.domain.VehicleRoutingSolution;
import org.acme.vehiclerouting.persistence.Solution;
import org.acme.vehiclerouting.persistence.VehicleRoutingSolutionsRepository;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/vrp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SolverResource {

    private final AtomicReference<Throwable> solverError = new AtomicReference<>();

    private final VehicleRoutingSolutionsRepository repository;
    private final SolverManager<VehicleRoutingSolution, Long> solverManager;
    private final SolutionManager<VehicleRoutingSolution, HardSoftLongScore> solutionManager;

    public SolverResource(VehicleRoutingSolutionsRepository repository, SolverManager<VehicleRoutingSolution, Long> solverManager,
            SolutionManager<VehicleRoutingSolution, HardSoftLongScore> solutionManager) {
        this.repository = repository;
        this.solverManager = solverManager;
        this.solutionManager = solutionManager;
    }

    private Status statusFromSolution(Solution solution) {
        return new Status(solution, solutionManager.explain(solution.getVehicleRoutingSolution()).getSummary(),
                this.repository.getSolutions().stream().map(Solution::getId).map(solverManager::getSolverStatus).collect(Collectors.toSet()),
                this.repository.getIteration());
    }

    @GET
    @Path("status")
    public Status status() {
        Optional.ofNullable(solverError.getAndSet(null)).ifPresent(throwable -> {
            throw new RuntimeException("Solver failed", throwable);
        });

        Solution s = repository.getBestSolution().orElse(new Solution(VehicleRoutingSolution.empty(), 0));
        return statusFromSolution(s);
    }

    @POST
    @Path("solve")
    public void solve() {
        repository.setInitialRefSetDivisionFactor(2.0);
        repository.setRefSetDivisionFactor(1.0);
        new Thread(() -> {
            long start = System.currentTimeMillis();
            while ((System.currentTimeMillis() - start) < 300 * 1e3) {
                if (repository.isIntialPopulation()) {
                    repository.getSolutions().forEach(s -> solverManager.solveAndListen(s.getId(), problemId -> s.getVehicleRoutingSolution(),
                            s::setVehicleRoutingSolution, (problemId, throwable) -> solverError.set(throwable)));
                } else {
                    // ensure refSet has proper content and size
                    repository.updateRefSet();
                    Set<Solution> newSolutions = repository.generateNewSolutions();
                    System.err.println("new solutions: " + newSolutions.size());

                    newSolutions.forEach(s -> solverManager.solveAndListen(s.getId(), problemId -> s.getVehicleRoutingSolution(), s::setVehicleRoutingSolution,
                            (problemId, throwable) -> solverError.set(throwable)));
                }
                while (!repository.getSolutions().stream().map(Solution::getId).map(solverManager::getSolverStatus).distinct().collect(Collectors.toSet())
                        .equals(Set.of(SolverStatus.NOT_SOLVING))) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                repository.incrementIteration();
            }
            System.err.println("Time used: " + (System.currentTimeMillis() - start));
        }).start();
    }

    @POST
    @Path("stopSolving")
    public void stopSolving() {
        repository.getSolutions().stream().map(Solution::getId).forEach(solverManager::terminateEarly);
    }
}
