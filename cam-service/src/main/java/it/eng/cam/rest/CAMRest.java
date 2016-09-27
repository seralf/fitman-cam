package it.eng.cam.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import it.eng.ontorepo.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.glassfish.jersey.server.ResourceConfig;

import it.eng.cam.rest.dto.AssetJSON;
import it.eng.cam.rest.dto.AssetModelJSON;
import it.eng.cam.rest.dto.AttributeJSON;
import it.eng.cam.rest.dto.ClassJSON;
import it.eng.cam.rest.dto.OwnerJSON;
import it.eng.cam.rest.dto.RelationshipJSON;
import it.eng.cam.rest.sesame.SesameRepoManager;

@Path("/")
public class CAMRest extends ResourceConfig {
    private static final Logger logger = LogManager.getLogger(CAMRest.class.getName());

    public CAMRest() {
        packages("it.eng.cam.rest");
    }

    // CLASSES

    /**
     * author ascatox the param flat = true gives a FLAT list of all classes
     * @param flat
     * @return
     */
    @GET
    @Path("/classes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClassItem> getClassHierarchy(@QueryParam("flat") boolean flat) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            return CAMRestImpl.getClasses(repoInstance, true, flat);
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/classes/{className}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClassItem> getIndividuals(@PathParam("className") String className) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<ClassItem> classes = CAMRestImpl.getClasses(repoInstance, false, false);
            ClassItem deepSearchClass = CAMRestImpl.deepSearchClasses(classes, className);
            if (deepSearchClass == null)
                return new ArrayList<ClassItem>();
            return deepSearchClass.getSubClasses();
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @POST
    @Path("/classes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClass(ClassJSON clazz) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.createClass(repoInstance, clazz.getName(), clazz.getParentName());
            return Response.ok("Class with name '" + clazz.getName() + "' was successfully created!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @PUT
    @Path("/classes/{className}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateClass(@PathParam("className") String className, ClassJSON clazz) {
        RepositoryDAO repoInstance = SesameRepoManager.getRepoInstance(getClass());
        try {
            if (!className.equalsIgnoreCase(clazz.getName())) {
                try {
                    CAMRestImpl.renameClass(repoInstance, className, clazz.getName());
                } catch (Exception e) {
                    logger.error(e);
                    return createResponseError(e.getMessage());
                } finally {
                    SesameRepoManager.releaseRepoDaoConn(repoInstance);
                }
                className = clazz.getName();
            }
            CAMRestImpl.moveClass(repoInstance, className, clazz.getParentName());
            return Response.ok("Class with name '" + className + "' has parent Class " + clazz.getParentName()).build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @DELETE
    @Path("/classes/{className}")
    public Response deleteClass(@PathParam("className") String className) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.deleteClass(repoInstance, className);
            return Response.ok("Class with name '" + className + "' was successfully deleted!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    // FINE CLASSES
    // ASSETS

    //Modified in 2016-09-22 by ascatox
    //Models as template of assets doesn't exist anymore.
    @GET
    @Path("/assets/{assetName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IndividualItem> getAssetByName(@PathParam("assetName") String assetName) {
        final RepositoryDAO repoInstance = SesameRepoManager.getRepoInstance(getClass());
        try {
            List<IndividualItem> assets = CAMRestImpl.getIndividuals(repoInstance);
            if (null == assetName || "".equals(assetName.trim()))
                return assets.stream()
                        .filter(asset ->
                                asset.getNamespace().equalsIgnoreCase(SesameRepoManager.getNamespace()))
                        .collect(Collectors.toList());
            return assets.stream().filter(asset -> asset.getNormalizedName().equalsIgnoreCase(assetName))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/assets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IndividualItem> getAssetsForClass(@QueryParam("className") String className,
                                                  @QueryParam("retrieveForChildren") boolean retrieveForChildren) {
        final RepositoryDAO repoInstance = SesameRepoManager.getRepoInstance(getClass());
        try {
            if (null == className)
                return getAssetByName(null);
            if (retrieveForChildren)
                return CAMRestImpl.getIndividualsForChildren(repoInstance, className);
            return CAMRestImpl.getIndividuals(repoInstance, className);
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @POST
    @Path("/assets")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAsset(AssetJSON asset) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.createAsset(repoInstance, asset.getName(), asset.getModelName(), asset.getOwnerName());
            return Response.ok("Asset with name '" + asset.getName() + "' for Model '" + asset.getModelName()
                    + "' for Owner '" + asset.getOwnerName() + "' was successfully created!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @PUT
    @Path("/assets/{assetName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAsset(@PathParam("assetName") String assetName, AssetJSON asset) {
        List<PropertyValueItem> individualAttributes = null;
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance, assetName);
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
        if (individualAttributes != null) {
            try {
                repoInstance = SesameRepoManager.getRepoInstance(getClass());
                CAMRestImpl.deleteIndividual(repoInstance, assetName);
            } catch (Exception e) {
                logger.error(e);
                return createResponseError(e.getMessage());
            } finally {
                SesameRepoManager.releaseRepoDaoConn(repoInstance);
            }
            try {
                repoInstance = SesameRepoManager.getRepoInstance(getClass());
                CAMRestImpl.createAsset(repoInstance, asset.getName(), asset.getModelName(), asset.getOwnerName());
                return Response.ok("Asset with name '" + asset.getName() + "' for Model '" + asset.getModelName()
                        + "' for Owner '" + asset.getOwnerName() + "' was successfully updated!").build();

            } catch (Exception e) {
                logger.error(e);
                return createResponseError(e.getMessage());
            } finally {
                SesameRepoManager.releaseRepoDaoConn(repoInstance);
            }
        } else {
            return Response.notModified("Asset with name '" + asset.getName() + "' for Model '" + asset.getModelName()
                    + "' for Owner '" + asset.getOwnerName() + "' does not exist").build();
        }

    }

    @DELETE
    @Path("/assets/{assetName}")
    public Response deleteAsset(@PathParam("assetName") String assetName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            cleanProps(assetName);
            CAMRestImpl.deleteIndividual(repoInstance, assetName);
            return Response.ok("Individual with name '" + assetName + "' was successfully deleted!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/assets/{assetName}/attributes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PropertyValueItem> getIndividualAttributes(@PathParam("assetName") String assetName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance, assetName);
            return individualAttributes.stream()
                    .filter(item -> !item.getNormalizedValue().equals(OWL.OBJECTPROPERTY.stringValue())
                            || item.getNormalizedName().contains("system"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/assets/{assetName}/attributes/{attributeName}")
    @Produces(MediaType.APPLICATION_JSON)
    public PropertyValueItem getIndividualAttribute(@PathParam("assetName") String assetName,
                                                    @PathParam("attributeName") String attributeName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> attrs = CAMRestImpl.getIndividualAttributes(repoInstance, assetName);
            for (PropertyValueItem attr : attrs) {
                if (attr.getNormalizedName().equalsIgnoreCase(attributeName))
                    return attr;
            }
            return null;
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @POST
    @Path("/assets/{assetName}/attributes/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setAttribute(@PathParam("assetName") String assetName, AttributeJSON attribute) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.setAttribute(repoInstance, attribute.getName(), assetName, attribute.getValue(),
                    attribute.getType());
            return Response.ok("Attribute with name '" + attribute.getName() + "'for individual '" + attribute.getName()
                    + "' and value '" + attribute.getValue() + "' of type '" + attribute.getType()
                    + "' was successfully added!").build();
        } catch (IllegalArgumentException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @PUT
    @Path("/assets/{assetName}/attributes/{attributeName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setAttribute(@PathParam("assetName") String assetName,
                                 @PathParam("attributeName") String attributeName, AttributeJSON attribute) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.removeProperty(repoInstance, assetName, attributeName);
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.setAttribute(repoInstance, attribute.getName(), assetName, attribute.getValue(),
                    attribute.getType());
            return Response.ok(
                    "Attribute with name '" + attribute.getName() + "'for individual '" + assetName + "' and value '"
                            + attribute.getValue() + "' of type '" + attribute.getType() + "' was successfully added!")
                    .build();
        } catch (IllegalArgumentException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @DELETE
    @Path("/assets/{assetName}/attributes/{attributeName}")
    public Response removeAttribute(@PathParam("assetName") String assetName,
                                    @PathParam("attributeName") String attributeName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.removeProperty(repoInstance, assetName, attributeName);
            return Response.ok("Attribute with name '" + attributeName + "'for individual '" + assetName
                    + "' was successfully deleted!").build();
        } catch (IllegalArgumentException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/assets/{assetName}/relationships")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PropertyValueItem> getRelationships(@PathParam("assetName") String assetName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance, assetName);
            return individualAttributes.stream()
                    .filter(item -> item.getNormalizedValue().equals(OWL.OBJECTPROPERTY.stringValue())
                            && !item.getNormalizedName().contains("system"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/assets/{assetName}/relationships/{relationshipName}")
    @Produces(MediaType.APPLICATION_JSON)
    public PropertyValueItem getRelationship(@PathParam("assetName") String assetName,
                                             @PathParam("relationshipName") String relationshipName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance, assetName);
            for (PropertyValueItem propertyValueItem : individualAttributes) {
                if (propertyValueItem.getNormalizedName().equalsIgnoreCase(relationshipName)) {
                    return propertyValueItem;
                }
            }
            return null;
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @POST
    @Path("/assets/{assetName}/relationships")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRelationship(@PathParam("assetName") String assetName, RelationshipJSON relationship) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.setRelationship(repoInstance, relationship.getName(), assetName,
                    relationship.getReferredName());
            return Response.ok("Relation with name '" + relationship.getName() + "'between '" + assetName + "' and '"
                    + relationship.getReferredName() + "' was successfully created!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @PUT
    @Path("/assets/{assetName}/relationships/{relationshipName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRelationship(@PathParam("assetName") String assetName,
                                       @PathParam("relationshipName") String relationshipName, RelationshipJSON relationship) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.removeProperty(repoInstance, assetName, relationshipName);
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
        try {
            CAMRestImpl.setRelationship(repoInstance, relationship.getName(), assetName,
                    relationship.getReferredName());
            return Response.ok("Relation with name '" + relationshipName + "'between '" + assetName + "' and '"
                    + relationship.getReferredName() + "' was successfully updated!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @DELETE
    @Path("/assets/{assetName}/relationships/{relationshipName}")
    public Response deleteRelationship(@PathParam("assetName") String assetName,
                                       @PathParam("relationshipName") String relationshipName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.removeProperty(repoInstance, assetName, relationshipName);
            return Response.ok(
                    "Relation with name '" + relationshipName + "'with '" + assetName + "' was successfully deleted!")
                    .build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    // FINE ASSETS
    @GET
    @Path("/attributes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PropertyDeclarationItem> getAttributes() {
        final RepositoryDAO repoInstance = SesameRepoManager.getRepoInstance(getClass());
        try {
            return CAMRestImpl.getAttributes(repoInstance);
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }


    @GET
    @Path("/models/{modelName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IndividualItem> getModelByName(@PathParam("modelName") String modelName) {
        final RepositoryDAO repoInstance = SesameRepoManager.getRepoInstance(getClass());
        try {
            List<IndividualItem> models = CAMRestImpl.getIndividuals(repoInstance);
            if (null == modelName)
                return models.stream()
                        .filter(asset -> CAMRestImpl.isModel(repoInstance, getClass(), asset.getIndividualName()))
                        .collect(Collectors.toList());
            return models.stream()
                    .filter(model -> model.getNormalizedName().equalsIgnoreCase(modelName)
                            && CAMRestImpl.isModel(repoInstance, getClass(), model.getIndividualName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/models")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IndividualItem> getModelsForClass(@QueryParam("className") String className) {
        final RepositoryDAO repoInstance = SesameRepoManager.getRepoInstance(getClass());
        try {
            if (null == className)
                return getModelByName(null);
            List<IndividualItem> individuals = CAMRestImpl.getIndividuals(repoInstance, className);
            return individuals.stream()
                    .filter(indiv -> CAMRestImpl.isModel(repoInstance, getClass(), indiv.getIndividualName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @POST
    @Path("/models")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAssetModel(AssetModelJSON model) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.createAssetModel(repoInstance, model.getName(), model.getClassName(), model.getOwnerName());
            return Response.ok("Model with name '" + model.getName() + "' for Model '" + model.getClassName()
                    + "' for Owner '" + model.getOwnerName() + "' was successfully created!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @PUT
    @Path("/models/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAssetModel(@PathParam("modelName") String modelName, AssetModelJSON model) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance, modelName);
            if (individualAttributes != null) {
                try {
                    CAMRestImpl.deleteIndividual(repoInstance, modelName);
                } catch (Exception e) {
                    logger.error(e);
                    return createResponseError(e.getMessage());
                } finally {
                    SesameRepoManager.releaseRepoDaoConn(repoInstance);
                }
                try {
                    repoInstance = SesameRepoManager.getRepoInstance(getClass());
                    CAMRestImpl.createAssetModel(repoInstance, model.getName(), model.getClassName(),
                            model.getOwnerName());
                    return Response.ok("Model with name '" + model.getName() + "' for Model '" + model.getClassName()
                            + "' for Owner '" + model.getOwnerName() + "' was successfully updated!").build();
                } catch (Exception e) {
                    logger.error(e);
                    return createResponseError(e.getMessage());
                } finally {
                    SesameRepoManager.releaseRepoDaoConn(repoInstance);
                }
            } else {
                return Response.notModified("Model with name '" + model.getName() + "' for Model '"
                        + model.getClassName() + "' for Owner '" + model.getOwnerName() + "' does not exist").build();
            }
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @DELETE
    @Path("/models/{modelName}")
    public Response deleteModel(@PathParam("modelName") String modelName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            cleanProps(modelName);
            CAMRestImpl.deleteIndividual(repoInstance, modelName);
            return Response.ok("Model with name '" + modelName + "' was successfully deleted!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/models/{modelName}/attributes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PropertyValueItem> getModelAttributes(@PathParam("modelName") String modelName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance, modelName);
            return individualAttributes.stream()
                    .filter(item -> item.getNormalizedName().contains("system")
                            || !item.getNormalizedValue().equals(OWL.OBJECTPROPERTY.stringValue()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/models/{modelName}/attributes/{attributeName}")
    @Produces(MediaType.APPLICATION_JSON)
    public PropertyValueItem getModelAttribute(@PathParam("modelName") String modelName,
                                               @PathParam("attributeName") String attributeName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> attrs = CAMRestImpl.getIndividualAttributes(repoInstance, modelName);
            for (PropertyValueItem attr : attrs) {
                if (attr.getNormalizedName().equalsIgnoreCase(attributeName))
                    return attr;
            }
            return null;
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @POST
    @Path("/models/{modelName}/attributes/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setModelAttribute(@PathParam("modelName") String modelName, AttributeJSON attribute) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.setAttribute(repoInstance, attribute.getName(), modelName, attribute.getValue(),
                    attribute.getType());
            return Response.ok("Attribute with name '" + attribute.getName() + "'for individual '" + attribute.getName()
                    + "' and value '" + attribute.getValue() + "' of type '" + attribute.getType()
                    + "' was successfully added!").build();
        } catch (IllegalArgumentException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @PUT
    @Path("/models/{modelName}/attributes/{attributeName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setModelAttribute(@PathParam("modelName") String modelName,
                                      @PathParam("attributeName") String attributeName, AttributeJSON attribute) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.removeProperty(repoInstance, modelName, attributeName);
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
        try {
            CAMRestImpl.setAttribute(repoInstance, attribute.getName(), modelName, attribute.getValue(),
                    attribute.getType());
            return Response.ok(
                    "Attribute with name '" + attribute.getName() + "'for individual '" + modelName + "' and value '"
                            + attribute.getValue() + "' of type '" + attribute.getType() + "' was successfully added!")
                    .build();
        } catch (IllegalArgumentException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @DELETE
    @Path("/models/{modelName}/attributes/{attributeName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeModelAttribute(@PathParam("modelName") String modelName,
                                         @PathParam("attributeName") String attributeName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.removeProperty(repoInstance, modelName, attributeName);
            return Response.ok("Attribute with name '" + attributeName + "'for individual '" + modelName
                    + "' was successfully deleted!").build();
        } catch (IllegalArgumentException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/models/{modelName}/relationships")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PropertyValueItem> getModelRelationships(@PathParam("modelName") String modelName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance, modelName);
            return individualAttributes.stream()
                    .filter(item -> !item.getNormalizedName().contains("system")
                            && item.getNormalizedValue().equals(OWL.OBJECTPROPERTY.stringValue()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/models/{modelName}/relationships/{relationshipName}")
    @Produces(MediaType.APPLICATION_JSON)
    public PropertyValueItem getModelRelationship(@PathParam("modelName") String modelName,
                                                  @PathParam("relationshipName") String relationshipName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<PropertyValueItem> individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance, modelName);
            for (PropertyValueItem propertyValueItem : individualAttributes) {
                if (propertyValueItem.getNormalizedName().equalsIgnoreCase(relationshipName)) {
                    return propertyValueItem;
                }
            }
            return null;
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @POST
    @Path("/models/{modelName}/relationships")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createModelRelationship(@PathParam("modelName") String modelName, RelationshipJSON relationship) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.setRelationship(repoInstance, relationship.getName(), modelName,
                    relationship.getReferredName());
            return Response.ok("Relation with name '" + relationship.getName() + "'between '" + modelName + "' and '"
                    + relationship.getReferredName() + "' was successfully created!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @PUT
    @Path("/models/{modelName}/relationships/{relationshipName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateModelRelationship(@PathParam("modelName") String modelName,
                                            @PathParam("relationshipName") String relationshipName, RelationshipJSON relationship) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.removeProperty(repoInstance, modelName, relationshipName);
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.setRelationship(repoInstance, relationship.getName(), modelName,
                    relationship.getReferredName());
            return Response.ok("Relation with name '" + relationshipName + "'between '" + modelName + "' and '"
                    + relationship.getReferredName() + "' was successfully updated!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @DELETE
    @Path("/models/{modelName}/relationships/{relationshipName}")
    public Response deleteModelRelationship(@PathParam("modelName") String modelName,
                                            @PathParam("relationshipName") String relationshipName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.removeProperty(repoInstance, modelName, relationshipName);
            return Response.ok(
                    "Relation with name '" + relationshipName + "'with '" + modelName + "' was successfully deleted!")
                    .build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }
    // FINE MODELS

    // OWNERS

    @GET
    @Path("/owners/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OwnerJSON> getOwners() {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<OwnerJSON> retval = new ArrayList<OwnerJSON>();
            List<String> owners = CAMRestImpl.getOwners(repoInstance);
            // if (null == ownerName || "".equalsIgnoreCase(ownerName))
            owners.forEach(item -> retval.add(new OwnerJSON(item)));
            // else
            // owners.stream().filter(owner ->
            // owner.equalsIgnoreCase(ownerName)).collect(Collectors.toList())
            // .forEach(item -> retval.add(new OwnerJSON(item)));
            return retval;
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @GET
    @Path("/owners/{ownerName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OwnerJSON> getOwner(@PathParam("ownerName") String ownerName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            List<OwnerJSON> retval = new ArrayList<OwnerJSON>();
            List<String> owners = CAMRestImpl.getOwners(repoInstance);
            if (null == ownerName || "".equalsIgnoreCase(ownerName))
                owners.forEach(item -> retval.add(new OwnerJSON(item)));
            else
                owners.stream().filter(owner -> owner.equalsIgnoreCase(ownerName)).collect(Collectors.toList())
                        .forEach(item -> retval.add(new OwnerJSON(item)));
            return retval;
        } catch (Exception e) {
            logger.error(e);
            throw new WebApplicationException(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @POST
    @Path("/owners/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOwner(OwnerJSON owner) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.createOwner(repoInstance, owner.getName());
            return Response.ok("Owner with name '" + owner.getName() + "' was successfully created!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @PUT
    @Path("/owners/{ownerName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOwner(@PathParam("ownerName") String ownerName, OwnerJSON owner) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.deleteOwner(repoInstance, ownerName);
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.createOwner(repoInstance, owner.getName());
            return Response.ok("Owner with name '" + owner.getName() + "' was successfully updated!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    @DELETE
    @Path("/owners/{ownerName}")
    public Response deleteOwner(@PathParam("ownerName") String ownerName) {
        RepositoryDAO repoInstance = null;
        try {
            repoInstance = SesameRepoManager.getRepoInstance(getClass());
            CAMRestImpl.deleteOwner(repoInstance, ownerName);
            return Response.ok("Owner with name '" + ownerName + "' was successfully deleted!").build();
        } catch (Exception e) {
            logger.error(e);
            return createResponseError(e.getMessage());
        } finally {
            SesameRepoManager.releaseRepoDaoConn(repoInstance);
        }
    }

    // FINE OWNERS

    // REST Utilities

    @GET
    @Path("/classes/ancestors/{className}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getTreePath(@PathParam("className") String className) {
        if (null == className || "".equalsIgnoreCase(className.trim()))
            return new ArrayList<String>();
        return CAMRestImpl.getTreePath(className);
    }

    @GET
    @Produces("text/html")
    public String summary(@Context HttpServletRequest httpRequest) {
        String content = "";
        StringBuilder contentBuilder = new StringBuilder();
        ResourceBundle finder = ResourceBundle.getBundle("cam-service");
        try {
            URL url = getClass().getResource("/summary.html");
            File file = new File(url.toURI());
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
            }
            in.close();
            content = contentBuilder.toString();
            String serverUrl = httpRequest.getServerName();
            int serverPort = httpRequest.getServerPort();
            String protocol = "http";
            if (serverPort == 443)
                protocol = "https";
            content = content.replaceAll("camServiceUrl",
                    protocol + "://" + serverUrl + ":" + serverPort + "/CAMService");
            content = content.replaceAll("artifactInfo",
                    finder.getString("artifact.id") + " v" + finder.getString("version"));
        } catch (IOException e) {
            logger.error(e);
        } catch (URISyntaxException e) {
            logger.error(e);
        } catch (Exception e) {
            logger.error(e);
        }
        return content;
    }

    private void cleanProps(String individualName) {
        RepositoryDAO repoInstance = SesameRepoManager.getRepoInstance(getClass());
        List<PropertyValueItem> individualAttributes = CAMRestImpl.getIndividualAttributes(repoInstance,
                individualName);
        for (PropertyValueItem propertyValueItem : individualAttributes) {
            try {
                repoInstance = SesameRepoManager.getRepoInstance(getClass());
                if (!propertyValueItem.getNormalizedName().contains("#"))
                    CAMRestImpl.removeProperty(repoInstance, individualName, propertyValueItem.getNormalizedName());
            } catch (Exception e) {
                logger.error(e);
                throw new RuntimeException(e);
            } finally {
                SesameRepoManager.releaseRepoDaoConn(repoInstance);
            }
        }
    }

    private Response createResponseError(String message) {
        return Response.status(500).entity(message).type("text/plain").build();
    }
}