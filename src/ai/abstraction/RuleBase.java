package ai.abstraction;

import java.util.ArrayList;
import java.util.List;

public class RuleBase 
{
	List<Rule> rules = new ArrayList<Rule>();

	public void createRules()
	{		
		Rule r1 = new Rule();
		r1.pattern = "Barracks";
		r1.actionID = 1;
		
		Rule r2 = new Rule();
		r2.pattern = "Light";
		r2.actionID = 2;
		
		Rule r3 = new Rule();
		r3.pattern = "Base";
		r3.actionID = 3;
		
		Rule r4 = new Rule();
		r4.pattern = "Worker";
		r4.actionID = 4;
		
		Rule r5 = new Rule();
		r5.pattern = "Train Ranged";
		r5.actionID = 5;
		
		Rule r6 = new Rule();
		r6.pattern = "Ranged";
		r6.actionID = 6;
		
		Rule r7 = new Rule();
		r7.pattern = "Train Heavy";
		r7.actionID = 7;
		
		Rule r8 = new Rule();
		r8.pattern = "Heavy";
		r8.actionID = 8;
		
		rules.add(r1);
		rules.add(r2);
		rules.add(r3);
		rules.add(r4);
		rules.add(r5);
		rules.add(r6);
		rules.add(r7);
		rules.add(r8);
	}
}
