package at.jku.isse.ecco.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

import at.jku.isse.ecco.core.Association;
import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.storage.mem.module.MemModule;
import at.jku.isse.ecco.storage.mem.module.MemModuleRevision;

public class Condition {
	private final String conditionString;
	private String origConditionString;

	public Condition(String conditionString) {
		boolean start = false;
		int counter = 0;
		for (int i = 0; i < conditionString.length(); i++) {
			char actual = conditionString.charAt(i);	
			if (!start && Character.isLetterOrDigit(actual)) {
				start = true;
				conditionString = conditionString.substring(0, i) + "\"" + conditionString.substring(i);
				i++;
			}
			else if (start && actual == '(') counter++;
			else if (start && actual == ')') counter--;
			
			if (start && (counter < 0 || actual == '&' || actual == '|')) {
				counter = 0;
				start = false;
				conditionString = conditionString.substring(0, i) + "\"" + conditionString.substring(i);
				i++;
			}
		}
		
		conditionString = conditionString.replace("||", "|");
		conditionString = conditionString.replace("&&", "&");
		Expression<String> expr = ExprParser.parse(conditionString);
		expr = RuleSet.simplify(expr);
		expr = RuleSet.toDNF(expr);
		this.conditionString = expr.toString();
	}
	
	public Condition(String conditionString, Association association) {
		this("(" + conditionString + ")" + "&&(" + association.computeCondition().getPreprocessorConditionString()+")");
		this.origConditionString = conditionString;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((conditionString == null) ? 0 : conditionString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Condition other = (Condition) obj;
		if (conditionString == null) {
			if (other.conditionString != null)
				return false;
		} else if (!conditionString.equals(other.conditionString))
			return false;
		return true;
	}

	public Condition negate(Association association) {
		return new Condition("!(" + origConditionString + ")&&(" + association.computeCondition().getPreprocessorConditionString() + ")");
	}
	

	public void set(Association.Op association, Repository.Op repository) {
		String[] conjunctions = conditionString.split("\\|");
		for (String conjunction : conjunctions) {
			List<Set<Feature>> featureLists = addNewFeatures(conjunction, repository);
			MemModule module = new MemModule(featureLists.get(0).stream().toArray(Feature[]::new),
					featureLists.get(1).stream().toArray(Feature[]::new));
			FeatureRevision[] posFeatureRevisions = featureLists.get(0).stream().map(feature -> {
				return feature.getLatestRevision() != null ? feature.getLatestRevision()
						: feature.addRevision(UUID.randomUUID().toString());
			}).toArray(FeatureRevision[]::new);

			module.incCount();
			MemModuleRevision moduleRevision = module.addRevision(posFeatureRevisions,
					featureLists.get(1).stream().toArray(Feature[]::new));
			moduleRevision.incCount();
			association.addObservation(moduleRevision);
		}
	}

	private List<Set<Feature>> addNewFeatures(String condition, Repository.Op repository) {
		List<Set<Feature>> featureLists = new ArrayList<>(2);
		featureLists.add(new HashSet<>());
		featureLists.add(new HashSet<>());
		String[] features = condition.split("&");
		Arrays.stream(features)
		      .filter(f -> f.length() > 0)
		      .forEach(f -> {
		    	  	f = f.replaceAll("\\(|\\)| |\"", "");
		    	  	if (f.startsWith("!")) {
		    	  		f = f.substring(1);
		    	  		Feature feature = repository.addFeature("" + f.hashCode(), f);
		    	  		featureLists.get(1).add((feature == null ? repository.getFeature("" + f.hashCode()) : feature));
		    	  	} else {
		    	  		Feature feature = repository.addFeature("" + f.hashCode(), f);
		    	  		featureLists.get(0).add((feature == null ? repository.getFeature("" + f.hashCode()) : feature));
		    	  	}
		      });
		return featureLists;
	}
	
	@Override
	public String toString() {
		return this.conditionString;
	}
}
