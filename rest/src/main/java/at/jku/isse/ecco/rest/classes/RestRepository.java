package at.jku.isse.ecco.rest.classes;

import at.jku.isse.ecco.core.Commit;
import at.jku.isse.ecco.core.Variant;
import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.service.EccoService;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

//@Component
public class RestRepository implements Serializable {
    private final String name;
    private final EccoService service;


    public RestRepository(EccoService service, String name) {
        this.name = name;
        this.service = service;
    }

    public Collection<? extends RestFeature> getFeatures() {
        Collection<RestFeature> features = new LinkedList<>();
        for (Feature f : service.getRepository().getFeatures()) {
            features.add(new RestFeature(f));
        }
        return features;
    }

    public String getName() {
        return name;
    }

    public Collection<RestCommit> getCommits () {
        Collection<RestCommit> commits = new LinkedList<>();
        for (Commit c : service.getRepository().getCommits()) {
            commits.add(new RestCommit(c));
        }
        return commits;
    }

    public Collection<RestVariant> getVariants () {
        Collection<RestVariant> variants = new LinkedList<>();
        for (Variant v : service.getRepository().getVariants()) {
            variants.add(new RestVariant(v));
        }
        return variants;
    }
}

    //private Collection<Association.Op> associations;
    //private List<Map<MemModule, MemModule>> modules;






