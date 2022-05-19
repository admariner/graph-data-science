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
package org.neo4j.gds;

import org.neo4j.gds.core.utils.progress.JobId;
import org.neo4j.gds.core.utils.progress.LocalTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.TaskStore;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.TaskProgressTracker;
import org.neo4j.gds.core.utils.warnings.EmptyUserLogRegistryFactory;
import org.neo4j.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.neo4j.gds.core.utils.progress.tasks.Task.UNKNOWN_VOLUME;

public class InspectableTestProgressTracker extends TaskProgressTracker {

    private final TaskStore taskStore;
    private final JobId jobId;
    private final String userName;
    private final List<Optional<Task>> taskTrees = new ArrayList<>();

    public InspectableTestProgressTracker(
        Task baseTask,
        Log log,
        int concurrency,
        String userName, JobId jobId,
        TaskStore taskStore
    ) {
        super(baseTask, log, concurrency, jobId, new LocalTaskRegistryFactory(userName, taskStore), EmptyUserLogRegistryFactory.INSTANCE);
        this.userName = userName;
        this.jobId = jobId;
        this.taskStore = taskStore;
    }

    @Override
    public void logProgress(long progress) {
        super.logProgress(progress);
    }

    @Override
    public void beginSubTask() {
        super.beginSubTask();
        taskTrees.add(taskStore.query(userName, jobId).map(Task::clone));
    }

    @Override
    public void endSubTask() {
        super.endSubTask();
        taskTrees.add(taskStore.query(userName, jobId).map(Task::clone));
    }

    @Override
    public void setVolume(long volume) {
        super.setVolume(volume);
    }

    public void assertValidProgressEvolution() {
        assertThat(taskTrees).isNotEmpty();
        assertThat(taskTrees.get(0)).isPresent();
        var previousProgress = taskTrees.get(0).get().getProgress();
        var initialVolume = previousProgress.volume();
        assertThat(initialVolume).isNotEqualTo(UNKNOWN_VOLUME);
        assertThat(previousProgress.progress()).isEqualTo(0);
        for (Optional<Task> maybeTask : taskTrees.subList(1, taskTrees.size())) {
            if (maybeTask.isPresent()) {
                var progress = maybeTask.get().getProgress();
                assertThat(progress.volume()).isEqualTo(initialVolume);
                assertThat(progress.progress()).isGreaterThanOrEqualTo(previousProgress.progress());
                previousProgress = progress;
            }
        }
        assertThat(previousProgress.progress()).isEqualTo(previousProgress.volume());
    }
}