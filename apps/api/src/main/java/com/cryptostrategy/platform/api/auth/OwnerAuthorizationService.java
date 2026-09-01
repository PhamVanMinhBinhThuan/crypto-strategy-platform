package com.cryptostrategy.platform.api.auth;

import com.cryptostrategy.platform.api.error.ResourceInaccessibleException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Resolves owner-scoped resources without exposing their existence to another user. */
@Service
public final class OwnerAuthorizationService {

    public <ResourceId, Resource> Resource requireOwned(
            UUID authenticatedUserId,
            ResourceId resourceId,
            OwnedResourceLookup<ResourceId, Resource> publishedPort) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(publishedPort, "publishedPort");
        return requireAccessible(publishedPort.findOwned(authenticatedUserId, resourceId));
    }

    public <ParentId, ChildId, Resource> Resource requireOwnedChild(
            UUID authenticatedUserId,
            ParentId parentId,
            ChildId childId,
            ParentOwnedResourceLookup<ParentId, ChildId, Resource> publishedPort) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        Objects.requireNonNull(parentId, "parentId");
        Objects.requireNonNull(childId, "childId");
        Objects.requireNonNull(publishedPort, "publishedPort");
        return requireAccessible(publishedPort.findOwned(
                authenticatedUserId, parentId, childId));
    }

    private static <Resource> Resource requireAccessible(Optional<Resource> resource) {
        return Objects.requireNonNull(resource, "publishedPort result")
                .orElseThrow(ResourceInaccessibleException::new);
    }

    @FunctionalInterface
    public interface OwnedResourceLookup<ResourceId, Resource> {
        Optional<Resource> findOwned(UUID authenticatedUserId, ResourceId resourceId);
    }

    @FunctionalInterface
    public interface ParentOwnedResourceLookup<ParentId, ChildId, Resource> {
        Optional<Resource> findOwned(
                UUID authenticatedUserId,
                ParentId parentId,
                ChildId childId);
    }
}
