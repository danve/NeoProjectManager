package com.neoprojectmanager.model;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Iterator;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * The factory is used to generate all the first-level domain model objects.
 * Projects, for example, as a project can exist indipendently from other
 * objects. A Task should be generated by a Project instead, as a Task should
 * alsais belogn to a project. It evnelops the db instance we are actually
 * working on. And allow to manage in one location only the inizialization of
 * those objects.
 * 
 * @author xan
 * 
 */
public class Factory {
	enum RELATIONSHIP implements RelationshipType {
		META_PROJECT, META_RESOURCE, IS_A_PROJECT, IS_A_RESOURCE
	}

	private GraphDatabaseService gdbs = null;
	private String dbFolder = null;
	private Node projectRefNode;
	private Node resourceRefNode;

	private void initDB(GraphDatabaseService gdbs) {
		projectRefNode = getRefNode(gdbs, RELATIONSHIP.META_PROJECT);
		resourceRefNode = getRefNode(gdbs, RELATIONSHIP.META_RESOURCE);
	}

	private Node getRefNode(GraphDatabaseService gdbs, RelationshipType refRelType) {
		Relationship refRel = gdbs.getReferenceNode().getSingleRelationship(refRelType, Direction.OUTGOING);
		if (refRel == null) {
			Node refNode = gdbs.createNode();
			gdbs.getReferenceNode().createRelationshipTo(refNode, refRelType);
			return refNode;
		} else
			return refRel.getEndNode();
	}

	public Factory(String dbFolder) {
		this.dbFolder = dbFolder;
		if (gdbs == null)
			this.gdbs = new EmbeddedGraphDatabase(this.dbFolder);
		Transaction tx = gdbs.beginTx();
		try {
			initDB(gdbs);
			tx.success();
		} finally {
			tx.finish();
		}
	}

	public Iterator<Project> getAllProjects() {
		return NodeWrapper.getNodeWrapperIterator(Project.class, gdbs, projectRefNode, Order.BREADTH_FIRST,
				StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, new RelTup(RELATIONSHIP.IS_A_PROJECT,
						Direction.OUTGOING));
	}

	public Iterator<Resource> getAllResources() {
		return NodeWrapper.getNodeWrapperIterator(Resource.class, gdbs, resourceRefNode, Order.BREADTH_FIRST,
				StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, new RelTup(RELATIONSHIP.IS_A_RESOURCE,
						Direction.OUTGOING));
	}

	/**
	 * shutdown the database.
	 */
	public void close() {
		this.gdbs.shutdown();
		this.gdbs = null;
		this.resourceRefNode = null;
		this.projectRefNode = null;
	}

	public Project createProject(String name) {
		if (isBlank(name))
			throw new IllegalArgumentException("Name can not be blank");
		Transaction tx = this.gdbs.beginTx();
		try {
			Project n = new Project(name, this.gdbs);
			projectRefNode.createRelationshipTo(n.getNode(), RELATIONSHIP.IS_A_PROJECT);
			tx.success();
			return n;
		} finally {
			tx.finish();
		}
	}

	public Resource createResource(String name) {
		if (isBlank(name))
			throw new IllegalArgumentException("Name can not be blank");
		Transaction tx = this.gdbs.beginTx();
		try {
			Resource r = new Resource(name, this.gdbs);
			resourceRefNode.createRelationshipTo(r.getNode(), RELATIONSHIP.IS_A_RESOURCE);
			tx.success();
			return r;
		} finally {
			tx.finish();
		}
	}

	public Project getProjectById(long id) {
		Transaction tx = this.gdbs.beginTx();
		try {
			Node n = this.gdbs.getNodeById(id);
			Project p = null;
			if (n.hasRelationship(RELATIONSHIP.IS_A_PROJECT, Direction.INCOMING))
				p = new Project(n, this.gdbs);
			tx.success();
			return p;
		} finally {
			tx.finish();
		}
	}

	public Resource getResourceById(long id) {
		Transaction tx = this.gdbs.beginTx();
		try {
			Node n = this.gdbs.getNodeById(id);
			Resource p = null;
			if (n.hasRelationship(RELATIONSHIP.IS_A_RESOURCE, Direction.INCOMING))
				p = new Resource(n, this.gdbs);
			tx.success();
			return p;
		} finally {
			tx.finish();
		}
	}

	public void clearDB() {
		Transaction tx = gdbs.beginTx();
		try {
			for (Node n : gdbs.getAllNodes()) {
				for (Relationship r : n.getRelationships()) {
					r.delete();
				}
				if (n.getId() != 0)
					n.delete();
			}
			projectRefNode = null;
			resourceRefNode = null;
			initDB(gdbs);
			tx.success();
		} finally {
			tx.finish();
		}
	}

	public Transaction beginTx() {
		return gdbs.beginTx();
	}

	public void populateDB() {
		Transaction tx = gdbs.beginTx();
		try {
			/* create Projects
			 * create subprojects
			 * add Task 
			 * set Task dependencies
			 * set properties
			 * create resources
			 * assign resources */

			Project project1 = createProject("Project 1");
			Task task1 = project1.createTask("TaskImpl 1");
			Task task2 = project1.createTask("TaskImpl 2");
			task1.setDurationInMinutes(60 * 24 * 10);
			task2.setDurationInMinutes(60 * 24 * 6);
			task2.addDependentOn(task1);
			tx.success();
		} finally {
			tx.finish();
		}
	}

	public String getDbFolder() {
		return dbFolder;
	}
}
