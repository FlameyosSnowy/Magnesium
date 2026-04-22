package net.magnesiumbackend.test.lifecycle;

import net.magnesiumbackend.core.annotations.enums.LifecycleStage;
import net.magnesiumbackend.core.lifecycle.LifecycleDefinition;
import net.magnesiumbackend.core.lifecycle.LifecycleRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for LifecycleRegistry.
 */
class LifecycleRegistryTest {

    @Test
    void emptyRegistry() {
        LifecycleRegistry registry = new LifecycleRegistry();
        assertTrue(registry.getDefinitions().isEmpty());
        assertEquals(0, registry.getDefinitions().size());
    }

    @Test
    void registerStartable() {
        LifecycleRegistry registry = new LifecycleRegistry();
        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.INIT)
            .component(ServiceA.class)
            .onInitialize(serviceA -> ((ServiceA) serviceA).start())
            .build());

        assertFalse(registry.getDefinitions().isEmpty());
        assertEquals(1, registry.getDefinitions().size());
    }

    @Test
    void registerWithBuilder() {
        LifecycleRegistry registry = new LifecycleRegistry();
        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.READY)
            .component(ServiceA.class)
            .onInitialize(serviceA -> ((ServiceA) serviceA).stop())
            .build());

        assertFalse(registry.getDefinitions().isEmpty());
    }

    @Test
    void definitionsReturnsImmutableList() {
        LifecycleRegistry registry = new LifecycleRegistry();
        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.INIT)
            .component(ServiceA.class)
            .onInitialize(serviceA -> ((ServiceA) serviceA).start())
            .build());

        List<LifecycleDefinition> defs = registry.getDefinitions();
        assertThrows(UnsupportedOperationException.class, () ->
            defs.clear()
        );
    }

    @Test
    void executeCallsLifecycleMethods() {
        LifecycleRegistry registry = new LifecycleRegistry();
        List<String> events = new ArrayList<>();

        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.INIT)
            .component(ServiceA.class)
            .onInitialize(svc -> {
                events.add("A-started");
                ((ServiceA) svc).start();
            })
            .build());

        ServiceA serviceA = new ServiceA();

        registry.execute(
            cls -> serviceA,
            err -> events.add("error: " + err.getMessage())
        );

        assertTrue(events.contains("A-started"));
        assertTrue(serviceA.started);
    }

    @Test
    void stagesExecuteInOrder() {
        LifecycleRegistry registry = new LifecycleRegistry();
        List<String> events = new ArrayList<>();

        // PRE_INIT stage - use ServiceC
        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.PRE_INIT)
            .component(ServiceC.class)
            .onInitialize(svc -> events.add("pre-init"))
            .build());

        // INIT stage - use ServiceA
        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.INIT)
            .component(ServiceA.class)
            .onInitialize(svc -> events.add("init"))
            .build());

        // POST_INIT stage - use ServiceD
        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.POST_INIT)
            .component(ServiceD.class)
            .onInitialize(svc -> events.add("post-init"))
            .build());

        registry.execute(
            cls -> {
                if (cls == ServiceA.class) return new ServiceA();
                if (cls == ServiceC.class) return new ServiceC();
                if (cls == ServiceD.class) return new ServiceD();
                return null;
            },
            err -> {}
        );

        assertEquals(List.of("pre-init", "init", "post-init"), events);
    }

    @Test
    void readyStageExecutes() {
        LifecycleRegistry registry = new LifecycleRegistry();
        List<String> events = new ArrayList<>();

        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.READY)
            .component(ServiceA.class)
            .onInitialize(svc -> {
                events.add("ready");
                ((ServiceA) svc).stop();
            })
            .build());

        ServiceA serviceA = new ServiceA();

        registry.execute(
            cls -> serviceA,
            err -> {}
        );

        assertTrue(events.contains("ready"));
        assertTrue(serviceA.stopped);
    }

    @Test
    void clearRemovesAllDefinitions() {
        LifecycleRegistry registry = new LifecycleRegistry();
        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.INIT)
            .component(ServiceA.class)
            .onInitialize(svc -> ((ServiceA) svc).start())
            .build());
        registry.clear();

        assertTrue(registry.getDefinitions().isEmpty());
    }

    @Test
    void multipleComponentsInSameStage() {
        LifecycleRegistry registry = new LifecycleRegistry();
        List<String> events = new ArrayList<>();

        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.INIT)
            .component(ServiceA.class)
            .onInitialize(svc -> events.add("A"))
            .build());
        registry.register(LifecycleDefinition.builder()
            .stage(LifecycleStage.INIT)
            .component(ServiceB.class)
            .onInitialize(svc -> events.add("B"))
            .build());

        registry.execute(
            cls -> cls == ServiceA.class ? new ServiceA() : new ServiceB(),
            err -> {}
        );

        assertEquals(2, events.size());
        assertTrue(events.contains("A"));
        assertTrue(events.contains("B"));
    }

    @Test
    void lifecycleStageEnumValues() {
        assertEquals(4, LifecycleStage.values().length);
        assertNotNull(LifecycleStage.valueOf("PRE_INIT"));
        assertNotNull(LifecycleStage.valueOf("INIT"));
        assertNotNull(LifecycleStage.valueOf("POST_INIT"));
        assertNotNull(LifecycleStage.valueOf("READY"));
    }

    // Test helper classes
    static class ServiceA {
        boolean started = false;
        boolean stopped = false;

        Void start() {
            started = true;
            return null;
        }

        Void stop() {
            stopped = true;
            return null;
        }
    }

    static class ServiceB {
        void start() {}
        void stop() {}
    }

    static class ServiceC {
        void start() {}
        void stop() {}
    }

    static class ServiceD {
        void start() {}
        void stop() {}
    }
}
