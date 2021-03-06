/*
 * Copyright (c) 2010. Alessandro Mecca <alessandro.mecca@gmail.com>
 * All rights reserved.
 *
 * See LICENSE file for details.
 */

package com.neoprojectmanager.model;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import static org.apache.commons.lang.StringUtils.isBlank;

public class Task extends NodeWrapper {
	enum PROPERTY {
		NAME, CREATED_ON, START_DATE, DURATION_IN_MINUTES, COST
	}

	enum RELATIONSHIP implements RelationshipType {
		DEPEND_ON, USE_RESOURCE
	}

	Task(Node node, GraphDatabaseService gdbs) {
		super(node, gdbs);
	}

	Task(String name, GraphDatabaseService gdbs) {
		super(gdbs);
		setName(name);
	}

	public TaskRelationship addDependentOn(Task other) {
		return new TaskRelationship(createRelationShip(other, RELATIONSHIP.DEPEND_ON, this), gdbs);
	}

	public TaskRelationship addDependent(Task other) {
		return new TaskRelationship(createRelationShip(this, RELATIONSHIP.DEPEND_ON, other), gdbs);
	}

	public Iterator<Task> getDependentTasks() {
		return getNodeWrapperIterator(Task.class, Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE,
				ReturnableEvaluator.ALL_BUT_START_NODE, new RelTup(RELATIONSHIP.DEPEND_ON, Direction.OUTGOING));
	}

	public Iterator<Resource> getResources() {
		return getNodeWrapperIterator(Resource.class, Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE,
				ReturnableEvaluator.ALL_BUT_START_NODE, new RelTup(RELATIONSHIP.USE_RESOURCE, Direction.OUTGOING));
	}

	public boolean hasDependents() {
		return hasRelationship(RELATIONSHIP.DEPEND_ON, Direction.OUTGOING);
	}

	public boolean hasDependencies() {
		return hasRelationship(RELATIONSHIP.DEPEND_ON, Direction.INCOMING);
	}

	public void setName(String name) {
		if (isBlank(name))
			throw new IllegalAccessError("Name can not be blank");
		setProperty(PROPERTY.NAME, name);
	}

	public String getName() {
		return (String) getPropertyOrNull(PROPERTY.NAME);
	}

	public void setStartDate(Date value) {
		if (value != null)
			setOrRemoveProperty(PROPERTY.START_DATE, value.getTime());
		else
			removeProperty(PROPERTY.START_DATE);
	}

	public Date getStartDate() {
		Long timeInMillis = (Long) getPropertyOrNull(PROPERTY.START_DATE);
		if (timeInMillis == null)
			return null;
		return new Date(timeInMillis);
	}

	/**
	 * @param durationInMinutes
	 */
	public void setDuration(Integer durationInMinutes) throws IllegalArgumentException {
		if (durationInMinutes != null && durationInMinutes < 0)
			throw new IllegalArgumentException("Duration must be positive.");
		setOrRemoveProperty(PROPERTY.DURATION_IN_MINUTES, durationInMinutes);
	}

	public Integer getDurationInMinutes() {
		return (Integer) getPropertyOrNull(PROPERTY.DURATION_IN_MINUTES);
	}

	public Project getProject() {
		Transaction tx = beginTx();
		try {
			Relationship r = getSingleRelationship(Project.RELATIONSHIP.INCLUDE_TASK, Direction.INCOMING);
			if (r == null)
				return null;
			Node n = r.getStartNode();
			tx.success();
			return new Project(n, this.gdbs);
		} finally {
			tx.finish();
		}
	}

	public Date getEndDate() {
		if (this.getStartDate() == null)
			return null;
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(this.getStartDate());
		Integer d = this.getDurationInMinutes();
		if (d == null) d = 0;
		endDate.add(Calendar.MINUTE, d);
		return endDate.getTime();
	}

	public String toString() {
		return "TaskImpl@" + getId();
	}

}
