package at.jku.isse.ecco.storage.neo4j.test;

import at.jku.isse.ecco.EccoService;
import at.jku.isse.ecco.storage.neo4j.domain.NeoAssociation;
import at.jku.isse.ecco.storage.neo4j.domain.NeoRepository;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class OpenRepoTest {

    private static final Path repoDir = Paths.get("src\\integrationTest\\data\\.ecco\\");

    @Test
    public void openRepo() throws InterruptedException {
        // create new repository
        EccoService service = new EccoService();
        service.setRepositoryDir(repoDir);
        service.open();
        NeoRepository repo = (NeoRepository) service.getRepository();
        System.out.println("Repository loaded.");

        repo.getFeatures();
        Collection<NeoAssociation.Op> associations = repo.getAssociations();
        associations.iterator().next().getRootNode();

        service.close();
        System.out.println("Repository closed.");

    }
}
