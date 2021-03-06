---
layout: default
title: Endpoint Mappings
parent: Type Mapping
nav_order: 20
---

# Endpoint Mappings

The global mapping variations are also available as explicit endpoint mappings. Instead of adding
the mapping in the global sections `map/types`, `map/parameters` and `map/responses` they can
be placed in the `map/paths` section as properties to an endpoint given by its path.

```yaml
    map:
    
      # path mappings, only valid for the given path
      paths:

        # the path
        /foo:

          # list of path specific mappings
          types:
            - from: ..
              to: ..

          # list of path specific parameter mappings, mapped by parameter name
          parameters:
            - name: ..
              to: ..

          # list of path specific content mappings, mapped by content type
          responses:    
            - content: ..
              to: ..

        # another path
        /foo2:

          # list of path specific mappings
          types:
            - from: ..
              to: ..

          # list of path specific parameter mappings, mapped by parameter name
          parameters:
            - name: ..
              to: ..

          # list of path specific content mappings, mapped by content type
          responses:    
            - content: ..
              to: ..

```

The mappings defined as properties of an endpoint will be used only for this endpoint. They don't
have any effect on other endpoints.
