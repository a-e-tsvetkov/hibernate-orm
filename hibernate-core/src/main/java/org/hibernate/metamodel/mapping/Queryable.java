/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.spi.DotIdentifierSequence;
import org.hibernate.boot.spi.SessionFactoryOptions;

/**
 * Defines a mapping model contract for things that can be queried in the HQL,
 * Criteria, etc sense.  Generally this
 *
 * @author Steve Ebersole
 */
public interface Queryable extends ModelPart {
	/**
	 * For an entity, this form allows for Hibernate's "implicit treat" support -
	 * meaning it should find a sub-part whether defined on the entity, its
	 * super-type or even one of its sub-types.
	 *
	 * @implNote Logically the implementation should consider
	 * {@link org.hibernate.jpa.spi.JpaCompliance}.  Not passed in because it
	 * is expected that implementors have access to the SessionFactory to access
	 * the JpaCompliance.  See {@link SessionFactoryOptions#getJpaCompliance}
	 */
	ModelPart findSubPart(String name, EntityMappingType treatTargetType);

	default ModelPart findByPath(String path) {
		int nextStart = 0;
		int dotIndex;
		Queryable modelPartContainer = this;
		while ( ( dotIndex = path.indexOf( '.', nextStart ) ) != -1 ) {
			modelPartContainer = (Queryable) modelPartContainer.findSubPart(
					path.substring( nextStart, dotIndex ),
					null
			);
			nextStart = dotIndex + 1;
		}
		return modelPartContainer.findSubPart( path.substring( nextStart ), null );
	}

	default ModelPart resolveSubPart(DotIdentifierSequence path) {
		return path.resolve(
				(ModelPart) this,
				(part, name) -> {
					final String fullPath = part.getNavigableRole().getFullPath();
					if ( fullPath.equals( name ) ) {
						return part;
					}
					else {
						return ( (Queryable) part ).findSubPart( name.substring( fullPath.length() + 1 ), null );
					}
				},
				(part, name) -> ( (Queryable) part ).findSubPart( name, null )
		);
	}

	/**
	 * For an entity, this form allows for Hibernate's "implicit treat" support -
	 * meaning it should find a sub-part whether defined on the entity or one of its sub-types.
	 *
	 * @implNote Logically the implementation should consider
	 * {@link org.hibernate.jpa.spi.JpaCompliance}.  Not passed in because it
	 * is expected that implementors have access to the SessionFactory to access
	 * the JpaCompliance.  See {@link SessionFactoryOptions#getJpaCompliance}
	 */
	default ModelPart findSubTypesSubPart(String name, EntityMappingType treatTargetType) {
		return findSubPart( name, treatTargetType );
	}

	/**
	 * Like {@link #findSubPart}, this form visits all parts defined on the
	 * entity, its super-types and its sub-types.
	 */
	void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType);

}
