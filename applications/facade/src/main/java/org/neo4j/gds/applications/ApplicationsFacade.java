/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.applications;

import org.neo4j.gds.algorithms.similarity.WriteRelationshipService;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmEstimationTemplate;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTemplate;
import org.neo4j.gds.applications.algorithms.machinery.ProgressTrackerCreator;
import org.neo4j.gds.applications.algorithms.machinery.RequestScopedDependencies;
import org.neo4j.gds.applications.graphstorecatalog.CatalogBusinessFacade;
import org.neo4j.gds.applications.graphstorecatalog.DefaultCatalogBusinessFacade;
import org.neo4j.gds.core.loading.GraphStoreCatalogService;
import org.neo4j.gds.logging.Log;
import org.neo4j.gds.metrics.projections.ProjectionMetricsService;

import java.util.Optional;
import java.util.function.Function;

/**
 * This is the top level facade for GDS applications. If you are integrating GDS,
 * this is the one thing you want to work with. See for example Neo4j Procedures.
 * <p>
 * We use the facade pattern for well known reasons,
 * and we apply a breakdown into sub-facades to keep things smaller and more manageable.
 */
public final class ApplicationsFacade {
    private final CatalogBusinessFacade catalogBusinessFacade;
    private final CentralityApplications centralityApplications;
    private final PathFindingApplications pathFindingApplications;
    private final SimilarityApplications similarityApplications;

    ApplicationsFacade(
        CatalogBusinessFacade catalogBusinessFacade,
        CentralityApplications centralityApplications,
        PathFindingApplications pathFindingApplications,
        SimilarityApplications similarityApplications
    ) {
        this.catalogBusinessFacade = catalogBusinessFacade;
        this.centralityApplications = centralityApplications;
        this.pathFindingApplications = pathFindingApplications;
        this.similarityApplications = similarityApplications;
    }

    /**
     * We can stuff all the boring structure stuff in here so nobody needs to worry about it.
     */
    public static ApplicationsFacade create(
        Log log,
        Optional<Function<CatalogBusinessFacade, CatalogBusinessFacade>> catalogBusinessFacadeDecorator,
        GraphStoreCatalogService graphStoreCatalogService,
        ProjectionMetricsService projectionMetricsService,
        AlgorithmEstimationTemplate algorithmEstimationTemplate,
        AlgorithmProcessingTemplate algorithmProcessingTemplate,
        RequestScopedDependencies requestScopedDependencies,
        WriteRelationshipService writeRelationshipService
    ) {
        var catalogBusinessFacade = createCatalogBusinessFacade(
            log,
            catalogBusinessFacadeDecorator,
            graphStoreCatalogService,
            projectionMetricsService
        );

        var progressTrackerCreator = new ProgressTrackerCreator(log, requestScopedDependencies);

        var centralityApplications = CentralityApplications.create(
            log,
            requestScopedDependencies,
            algorithmEstimationTemplate,
            algorithmProcessingTemplate,
            progressTrackerCreator
        );

        var pathFindingApplications = PathFindingApplications.create(
            log,
            requestScopedDependencies,
            algorithmProcessingTemplate,
            algorithmEstimationTemplate,
            progressTrackerCreator
        );

        var similarityApplications = SimilarityApplications.create(
            log,
            algorithmEstimationTemplate,
            algorithmProcessingTemplate,
            progressTrackerCreator,
            writeRelationshipService
        );

        return new ApplicationsFacadeBuilder()
            .with(catalogBusinessFacade)
            .with(centralityApplications)
            .with(pathFindingApplications)
            .with(similarityApplications)
            .build();
    }

    private static CatalogBusinessFacade createCatalogBusinessFacade(
        Log log,
        Optional<Function<CatalogBusinessFacade, CatalogBusinessFacade>> catalogBusinessFacadeDecorator,
        GraphStoreCatalogService graphStoreCatalogService,
        ProjectionMetricsService projectionMetricsService
    ) {
        var catalogBusinessFacade = DefaultCatalogBusinessFacade.create(
            log,
            graphStoreCatalogService,
            projectionMetricsService
        );

        if (catalogBusinessFacadeDecorator.isEmpty()) return catalogBusinessFacade;

        return catalogBusinessFacadeDecorator.get().apply(catalogBusinessFacade);
    }

    public CatalogBusinessFacade catalog() {
        return catalogBusinessFacade;
    }

    public CentralityApplications centrality() {
        return centralityApplications;
    }

    public PathFindingApplications pathFinding() {
        return pathFindingApplications;
    }

    public SimilarityApplications similarity() {
        return similarityApplications;
    }
}
