package ai.abstraction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;

public class RuleBasedSystem extends AbstractionLayerAI
{
	UnitTypeTable utt;
	UnitType workerType;
	UnitType baseType;
	UnitType barracksType;
	UnitType lightType;
	UnitType rangedType;
	UnitType heavyType;

	RuleBase rule = new RuleBase();

	public RuleBasedSystem(UnitTypeTable a_utt, PathFinding a_pf) 
	{
		super(a_pf);
		utt = a_utt;

		workerType = utt.getUnitType("Worker");
		baseType = utt.getUnitType("Base");
		barracksType = utt.getUnitType("Barracks");
		lightType = utt.getUnitType("Light");
		rangedType = utt.getUnitType("Ranged");
		heavyType = utt.getUnitType("Heavy");

		rule.createRules();
	}

	@Override
	public void reset() {}

	@Override
	public AI clone() 
	{
		return new RuleBasedSystem(utt, pf);
	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception 
	{
		KnowledgeBase kb = new KnowledgeBase();

		List<Rule> firedRules = new ArrayList<Rule>();

		perception(gs, player, kb);

		List<Rule> rules = rule.rules;

		for(int i=0; i<rules.size(); i++)
		{
			Rule r = rules.get(i);

			if(unification(r.pattern, kb))
			{
				firedRules.add(r);				
			}
		}

		executeRules(firedRules, player, gs);

		return translateActions(player, gs);
	}

	public boolean unification(String pattern, KnowledgeBase kb)
	{
		List<Predicate> facts = kb.facts;

		for(int i=0; i<facts.size(); i++)
		{
			Predicate p = facts.get(i);

			if(pattern.equals(p.functor))
				return true;
		}

		return false;
	}

	public void perception(GameState gs, int player, KnowledgeBase kb)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p1 = gs.getPlayer(player);

		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == workerType && u.getPlayer() == p1.getID())
			{
				Predicate p = new Predicate("Worker");
				kb.addPredicate(p);
			}

			if(u.getType() == baseType && u.getPlayer() == p1.getID())
			{
				Predicate p = new Predicate("Base");
				kb.addPredicate(p);
			}

			if(u.getType() == barracksType && u.getPlayer() == p1.getID())
			{
				Predicate p = new Predicate("Barracks");
				kb.addPredicate(p);
			}

			if(u.getType() == lightType && u.getPlayer() == p1.getID())
			{
				Predicate p = new Predicate("Light");
				kb.addPredicate(p);
			}

			if(countLight(player, gs) >= 1)
			{
				Predicate p = new Predicate("Train Ranged");
				kb.addPredicate(p);
			}

			if(u.getType() == rangedType && u.getPlayer() == p1.getID())
			{
				Predicate p = new Predicate("Ranged");
				kb.addPredicate(p);
			}

			//Train heavy when ranged are more than equal to 1
			if(countRanged(player, gs) >= 1)
			{
				Predicate p = new Predicate("Train Heavy");
				kb.addPredicate(p);
			}

			if(u.getType() == heavyType && u.getPlayer() == p1.getID())
			{
				Predicate p = new Predicate("Heavy");
				kb.addPredicate(p);
			}
		}
	}

	public void executeRules(List<Rule> firedRules, int player, GameState gs)
	{
		for(int i=0; i<firedRules.size(); i++)
		{
			Rule r = firedRules.get(i);

			if(r.actionID == 1)
			{
				//Train Light
				trainLight(player, gs);
			}

			if(r.actionID == 2)
			{
				//Attack nearest target
				attackLight(player, gs);
			}

			if(r.actionID == 3)
			{
				//Train worker
				trainWorker(player, gs, 3);
			}

			if(r.actionID == 4)
			{
				//Harvest resources and attack
				harvestBuild(player, gs);
			}

			if(r.actionID == 5)
			{
				//train Ranged units
				trainRanged(player, gs);
			}

			if(r.actionID == 6)
			{
				//Attack with ranged
				attackRanged(player, gs);
			}

			if(r.actionID == 7)
			{
				trainHeavy(player, gs);
			}

			if(r.actionID == 8)
			{
				attackHeavy(player, gs);
			}

		}
	}

	public void harvestBuild(int player, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);
		List<Unit> workers = new LinkedList<Unit>();

		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == workerType && u.getPlayer() == player)
				workers.add(u);
		}

		int nbases = 0;
		int nbarracks = 0;

		int resourcesUsed = 0;
		List<Unit> freeWorkers = new LinkedList<Unit>();
		freeWorkers.addAll(workers);

		if (workers.isEmpty()) 
			return;

		for (Unit u2 : pgs.getUnits()) 
		{
			if (u2.getType() == baseType
					&& u2.getPlayer() == p.getID())
			{
				nbases++;
			}
			if (u2.getType() == barracksType
					&& u2.getPlayer() == p.getID()) 
			{
				nbarracks++;
			}
		}

		List<Integer> reservedPositions = new LinkedList<Integer>();
		if (nbases == 0 && !freeWorkers.isEmpty()) 
		{
			// build a base:
			if (p.getResources() > baseType.cost + resourcesUsed) 
			{
				Unit u = freeWorkers.remove(0);
				int pos = findBuildingPosition(reservedPositions, u, p, pgs);
				build(u, baseType, pos % pgs.getWidth(), pos / pgs.getWidth());
				resourcesUsed += baseType.cost;
				reservedPositions.add(pos);
			}
		}

		if (nbarracks == 0) 
		{
			// build a barrack:
			if (p.getResources() > barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
				Unit u = freeWorkers.remove(0);
				int pos = findBuildingPosition(reservedPositions, u, p, pgs);
				build(u, barracksType, pos % pgs.getWidth(), pos / pgs.getWidth());
				resourcesUsed += baseType.cost;
			}
		}

		
		//If there are more than 2 workers then send 1 to attack
		if(freeWorkers.size() > 2)
		{
			List<Unit> attackUnits = new ArrayList<Unit>();

			attackUnits.add(freeWorkers.remove(0));
		//	attackUnits.add(freeWorkers.remove(1));
			//attackUnits.add(freeWorkers.remove(2));

			for(Unit u : attackUnits)
			{
				Unit closestEnemy = null;
				int closestDistance = 0;

				for(Unit u2 : pgs.getUnits())
				{
					if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
					{
						int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
						if (closestEnemy == null || d < closestDistance)
						{
							closestEnemy = u2;
							closestDistance = d;
						}
					}
				}

				if (closestEnemy != null) 
					attack(u, closestEnemy);
			}
		}
		 
		// harvest with all the free workers:
		for (Unit u : freeWorkers)
		{
			Unit closestBase = null;
			Unit closestResource = null;
			int closestDistance = 0;
			for (Unit u2 : pgs.getUnits()) {
				if (u2.getType().isResource) {
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if (closestResource == null || d < closestDistance) {
						closestResource = u2;
						closestDistance = d;
					}
				}
			}

			closestDistance = 0;
			for (Unit u2 : pgs.getUnits()) 
			{
				if (u2.getType().isStockpile && u2.getPlayer() == p.getID())
				{
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if (closestBase == null || d < closestDistance) 
					{
						closestBase = u2;
						closestDistance = d;
					}
				}
			}
			if (closestResource != null && closestBase != null) 
				harvest(u, closestResource, closestBase);

		}
	}

	public int findBuildingPosition(List<Integer> reserved, Unit u, Player p, PhysicalGameState pgs) {
		int bestPos = -1;
		int bestScore = 0;

		for (int x = 0; x < pgs.getWidth(); x++) {
			for (int y = 0; y < pgs.getHeight(); y++) {
				int pos = x + y * pgs.getWidth();
				if (!reserved.contains(pos) && pgs.getUnitAt(x, y) == null) {
					int score = 0;

					score = -(Math.abs(u.getX() - x) + Math.abs(u.getY() - y));

					if (bestPos == -1 || score > bestScore) {
						bestPos = pos;
						bestScore = score;
					}
				}
			}
		}

		return bestPos;
	}

	public void trainWorker(int player, GameState gs, int maxworkers)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);
		int nworkers = 0;
		for (Unit u2 : pgs.getUnits()) 
		{
			if (u2.getType() == workerType
					&& u2.getPlayer() == p.getID()) {
				nworkers++;
			}
		}
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == baseType && u.getPlayer() == player && gs.getActionAssignment(u) == null
					&& p.getResources() >= workerType.cost && nworkers < maxworkers)
				train(u, workerType);
		}
	}		

	public void trainRanged(int player, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);

		for (Unit u : pgs.getUnits())
		{
			if(u.getType() == barracksType && u.getPlayer() == player && p.getResources() >= rangedType.cost
					&& gs.getActionAssignment(u) == null)
				train(u, rangedType);
		}
	}

	public void trainLight(int player, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);

		for (Unit u : pgs.getUnits())
		{
			if(u.getType() == barracksType && u.getPlayer() == player && p.getResources() >= lightType.cost
					&& gs.getActionAssignment(u) == null)
				train(u, lightType);
		}
	}

	public void trainHeavy(int player, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);

		for (Unit u : pgs.getUnits())
		{
			if(u.getType() == barracksType && u.getPlayer() == player && p.getResources() >= heavyType.cost
					&& gs.getActionAssignment(u) == null)
				train(u, heavyType);
		}
	}

	public void attackRanged(int player, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == rangedType && u.getPlayer() == player && gs.getActionAssignment(u) == null)
			{
				Unit closestEnemy = null;
				int closestDistance = 0;

				for(Unit u2 : pgs.getUnits())
				{
					if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
					{
						int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
						if (closestEnemy == null || d < closestDistance)
						{
							closestEnemy = u2;
							closestDistance = d;
						}
					}
				}

				if (closestEnemy != null) 
					attack(u, closestEnemy);
			}
		}
	}

	public int countLight(int player, GameState gs)
	{
		int countLight = 0;
		PhysicalGameState pgs = gs.getPhysicalGameState();
		//	Player p = gs.getPlayer(player);
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == lightType && u.getPlayer() == player)
				countLight++;
		}

		return countLight;
	}

	public int countRanged(int player, GameState gs)
	{
		int countRanged = 0;
		PhysicalGameState pgs = gs.getPhysicalGameState();
		//	Player p = gs.getPlayer(player);
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == rangedType && u.getPlayer() == player)
				countRanged++;
		}

		return countRanged;
	}

	public void attackLight(int player, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == lightType && u.getPlayer() == player && gs.getActionAssignment(u) == null)
			{
				Unit closestEnemy = null;
				int closestDistance = 0;

				for(Unit u2 : pgs.getUnits())
				{
					if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
					{
						int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
						if (closestEnemy == null || d < closestDistance)
						{
							closestEnemy = u2;
							closestDistance = d;
						}
					}
				}

				if (closestEnemy != null) 
					attack(u, closestEnemy);
			}
		}
	}

	public void attackHeavy(int player, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == heavyType && u.getPlayer() == player && gs.getActionAssignment(u) == null)
			{
				Unit closestEnemy = null;
				int closestDistance = 0;

				for(Unit u2 : pgs.getUnits())
				{
					if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
					{
						int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
						if (closestEnemy == null || d < closestDistance)
						{
							closestEnemy = u2;
							closestDistance = d;
						}
					}
				}

				if (closestEnemy != null) 
					attack(u, closestEnemy);
			}
		}
	}
}

