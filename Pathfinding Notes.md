Pathfinding thought:

- Smaller grid squares
- Calculations performed by a manager not by each unit individually
- Collisions avoided by calculating where each unit will be on the grid in X frames and determining if a collision would happen at that point and adjusting
  accordingly
- Start with a simple path with no obstacles
- Detection of "when I am on this grid square in the future, I will be in range of an enemy" gives a unit a "target grid square" which immediately blocks it
  from other paths after X frames
- "Speed" determination in grid squares per second
- Attempt to "smooth" the path, rerouting once smoothed if collisions would occur
- Future: If moving to a grid square that another unit has targeted and that unit moving to an adjacent grid square would still allow both units to attack and
  the moving unit's closest path is that, make that happen

- Attack range: Must be adjacent, diagonally will use a small amount of movement to close the gap
- Aggro range: 5x5 square, increases by 2x2 each movement without a target, disabled when a valid target is found

- Go column by column, starting with column closest to center
  - Check if any unit in attack range and start attacking
  - If no current target set, check if any unit in aggro range and set as target
    - If target set by this, movement will transition from "move forward" to "move towards target". Aggro range now disabled until target dies
    - If no unit found in aggro range, increase aggro range radius by 0.5 grid square
  - Get next movement square
    - If has target, movement square will be next open square "towards" target. This may produce imperfect pathfinding which is ok for now
    - Otherwise, next is "one forward"
      - This follows the pattern of checking for open squares based on this decision tree:
        - X towards enemy side, no Y
        - XY towards enemy side
        - Y only, no X
        - XY backwards
        - X backwards only, no X
        - XY movement favors moving towards the middle of the arena over moving towards the edges
    - A square is "open" if the following are true:
      - A non-moving unit is not currently located in that square
      - A moving unit has not already targeted that square for its next movement
      - A unit moving diagonally will not intersect our diagonal movement
  - Initiate movement
    - For now, let's just make this a moveTo and all this inside a fixed updater. We'll smooth it out later

"Towards" Target pathfinding:

1. Get current Manhattan Distance
2. Follow "forward" pattern on open tiles

- If adjacent to, that's a good target so go now
3. Repeat once, removing the previous node from consideration
  - If no valid tile found, reset path and go back to 2, filtering out the attempted tile
  - If all valid tiles have a longer Manhattan Distance than from the previous tile, reset path and go back to 2, filtering out the attempted tile
  - Otherwise, the path has at least 2 valid steps
4. Return the "Next" node NB: Target can become "blocked" and we need to handle it

