package ai.abstraction;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeBase 
{
  List<Predicate> facts = new ArrayList<Predicate>();
  
  void addPredicate(Predicate p)
  {
	  facts.add(p);
  }
  
  void clear()
  {
	  facts.clear();
  }
}
