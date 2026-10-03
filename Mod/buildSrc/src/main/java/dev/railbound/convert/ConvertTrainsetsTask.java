package dev.railbound.convert;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

/** Gradle task: converts trainset and bogey art into generated resources; fails the build on any broken model rule. */
public abstract class ConvertTrainsetsTask extends DefaultTask {

    @Input
    public abstract Property<String> getNamespace();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getDesignDir();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getArtDir();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getBogeyDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void convert() {
        getProject().delete(getOutputDir());
        try {
            TrainsetBatch.Result result = TrainsetBatch.run(getNamespace().get(), getDesignDir().get().getAsFile().toPath(),
                    getArtDir().get().getAsFile().toPath(), getOutputDir().get().getAsFile().toPath());
            getLogger().lifecycle("Converted trainset models: {}", result.converted());
            getLogger().lifecycle("Converted bogey styles: {}", BogeyConverter.convertUsed(getNamespace().get(),
                    getDesignDir().get().getAsFile().toPath(), getBogeyDir().get().getAsFile().toPath(),
                    getOutputDir().get().getAsFile().toPath()));
            for (String id : result.withoutModel()) {
                getLogger().warn("Trainset design '{}' has no model in art/trainsets/{}/; it will render as a grey box", id, id);
            }
        } catch (ConversionException e) {
            throw new GradleException("Trainset model rules broken:\n" + e.getMessage());
        } catch (IOException e) {
            throw new GradleException("Could not convert trainset models", e);
        }
    }
}
