package mod.azuredoom.bettercrawling.common;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.Target;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public class CustomPathFinder extends PathFinder {
	private final BinaryHeap path = new BinaryHeap();
	private final Node[] pathOptions = new Node[32];
	private final NodeEvaluator nodeProcessor;

	private int maxExpansions = 200;

	public static interface Heuristic {
		public float compute(Node start, Node end, boolean isTargetHeuristic);
	}

	public static final Heuristic DEFAULT_HEURISTIC = (start, end, isTargetHeuristic) -> start.distanceManhattan(end); // distanceManhattan

	public Heuristic heuristic = DEFAULT_HEURISTIC;

	public CustomPathFinder(NodeEvaluator processor, int maxExpansions) {
		super(processor, maxExpansions);
		this.nodeProcessor = processor;
		this.maxExpansions = maxExpansions;
	}

	public NodeEvaluator getNodeProcessor() {
		return this.nodeProcessor;
	}

	public CustomPathFinder setMaxExpansions(int expansions) {
		this.maxExpansions = expansions;
		return this;
	}

	public CustomPathFinder setHeuristic(Heuristic heuristic) {
		this.heuristic = heuristic;
		return this;
	}

	@Nullable
	@Override
	public Path findPath(PathNavigationRegion region, Mob entity, Set<BlockPos> checkpoints, float maxDistance, int checkpointRange, float maxExpansionsMultiplier) {
		this.path.clear();
		this.nodeProcessor.prepare(region, entity);
		var pathpoint = this.nodeProcessor.getStart();
		// Create a checkpoint for each block pos in the checkpoints set
		Map<Target, BlockPos> checkpointsMap = checkpoints.stream().collect(Collectors.toMap((pos) -> {
			return this.nodeProcessor.getGoal(pos.getX(), pos.getY(), pos.getZ());
		}, Function.identity()));
		var path = this.findPath(pathpoint, checkpointsMap, maxDistance, checkpointRange, maxExpansionsMultiplier);
		this.nodeProcessor.done();
		return path;
	}

	// TODO Re-implement custom heuristics

	@Nullable
	private Path findPath(Node start, Map<Target, BlockPos> checkpointsMap, float maxDistance, int checkpointRange, float maxExpansionsMultiplier) {
		Set<Target> checkpoints = checkpointsMap.keySet();
		start.g = 0.0F;
		start.h = this.computeHeuristic(start, checkpoints);
		start.f = start.h;
		this.path.clear();
		this.path.insert(start);
		Set<Target> reachedCheckpoints = Sets.newHashSetWithExpectedSize(checkpoints.size());
		var expansions = 0;
		var maxExpansions = (int) (this.maxExpansions * maxExpansionsMultiplier);

		while (!this.path.isEmpty() && ++expansions < maxExpansions) {
			var openPathPoint = this.path.pop();
			openPathPoint.closed = true;
			for (var checkpoint : checkpoints)
				if (openPathPoint.distanceManhattan(checkpoint) <= checkpointRange) {
					checkpoint.setReached();
					reachedCheckpoints.add(checkpoint);
				}
			if (!reachedCheckpoints.isEmpty())
				break;
			if (openPathPoint.distanceTo(start) < maxDistance) {
				var numOptions = this.nodeProcessor.getNeighbors(this.pathOptions, openPathPoint);
				for (var i = 0; i < numOptions; ++i) {
					var successorPathPoint = this.pathOptions[i];
					var costHeuristic = openPathPoint.distanceTo(successorPathPoint); // TODO Replace with cost heuristic
					// walkedDistance corresponds to the total path cost of the evaluation function
					successorPathPoint.walkedDistance = openPathPoint.walkedDistance + costHeuristic;
					float totalSuccessorPathCost = openPathPoint.g + costHeuristic + successorPathPoint.costMalus;
					if (successorPathPoint.walkedDistance < maxDistance && (!successorPathPoint.inOpenSet() || totalSuccessorPathCost < successorPathPoint.g)) {
						successorPathPoint.cameFrom = openPathPoint;
						successorPathPoint.g = totalSuccessorPathCost;
						// distanceToNext corresponds to the heuristic part of the evaluation function
						successorPathPoint.h = this.computeHeuristic(successorPathPoint, checkpoints) * 1.0f; // TODO Vanilla's 1.5 multiplier is too greedy :( Move to custom heuristic stuff
						if (successorPathPoint.inOpenSet())
							this.path.changeCost(successorPathPoint, successorPathPoint.g + successorPathPoint.h);
						else {
							// distanceToTarget corresponds to the evaluation function, i.e. total path cost + heuristic
							successorPathPoint.f = successorPathPoint.g + successorPathPoint.h;
							this.path.insert(successorPathPoint);
						}
					}
				}
			}
		}
		Optional<Path> path;
		if (!reachedCheckpoints.isEmpty())
			// Use shortest path towards next reached checkpoint
			path = reachedCheckpoints.stream().map((checkpoint) -> {
				return this.createPath(checkpoint.getBestNode(), checkpointsMap.get(checkpoint), true);
			}).min(Comparator.comparingInt(Path::getNodeCount));
		else
			// Use lowest cost path towards any checkpoint
			path = checkpoints.stream().map((checkpoint) -> {
				return this.createPath(checkpoint.getBestNode(), checkpointsMap.get(checkpoint), false);
			}).min(Comparator.comparingDouble(Path::getDistToTarget /* TODO Replace calculation with cost heuristic */).thenComparingInt(Path::getNodeCount));
		return !path.isPresent() ? null : path.get();
	}

	private float computeHeuristic(Node pathPoint, Set<Target> checkpoints) {
		var minDst = Float.MAX_VALUE;
		for (var checkpoint : checkpoints) {
			float dst = pathPoint.distanceTo(checkpoint); // TODO Replace with target heuristic
			checkpoint.updateBest(dst, pathPoint);
			minDst = Math.min(dst, minDst);
		}
		return minDst;
	}

	protected Path createPath(Node start, BlockPos target, boolean isTargetReached) {
		List<Node> points = Lists.newArrayList();
		var currentPathPoint = start;
		points.add(0, start);
		while (currentPathPoint.cameFrom != null) {
			currentPathPoint = currentPathPoint.cameFrom;
			points.add(0, currentPathPoint);
		}
		return new Path(points, target, isTargetReached);
	}
}
