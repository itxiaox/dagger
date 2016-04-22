/*
 * Copyright (C) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger.internal.codegen;

import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.lang.model.util.Types;

/**
 * An {@link Element}, optionally contributed by a subtype of the type that encloses it.
 */
@AutoValue
abstract class SourceElement {

  /** An object that has a {@link SourceElement}. */
  interface HasSourceElement {
    /** The source element associated with this object. */
    SourceElement sourceElement();

    Function<SourceElement.HasSourceElement, SourceElement> SOURCE_ELEMENT =
        new Function<SourceElement.HasSourceElement, SourceElement>() {
          @Override
          public SourceElement apply(SourceElement.HasSourceElement hasSourceElement) {
            return hasSourceElement.sourceElement();
          }
        };
  }

  /** The {@link Element} instance.. */
  abstract Element element();

  /**
   * The concrete class that contributed the {@link #element()}, if different from
   * {@link #enclosingTypeElement()}.
   */
  abstract Optional<TypeElement> contributedBy();

  /** The type enclosing the {@link #element()}. */
  TypeElement enclosingTypeElement() {
    return BINDING_TYPE_ELEMENT.visit(element());
  }

  /**
   * The type of {@link #element()}, considered as a member of {@link #contributedBy()} if it is
   * present.
   */
  TypeMirror asMemberOfContributingType(Types types) {
    return contributedBy().isPresent()
        ? types.asMemberOf(MoreTypes.asDeclared(contributedBy().get().asType()), element())
        : element().asType();
  }

  private static final ElementVisitor<TypeElement, Void> BINDING_TYPE_ELEMENT =
      new SimpleElementVisitor6<TypeElement, Void>() {
        @Override
        protected TypeElement defaultAction(Element e, Void p) {
          return visit(e.getEnclosingElement());
        }

        @Override
        public TypeElement visitType(TypeElement e, Void p) {
          return e;
        }
      };

  static SourceElement forElement(Element element) {
    return new AutoValue_SourceElement(element, Optional.<TypeElement>absent());
  }

  static SourceElement forElement(Element element, TypeElement contributedBy) {
    return new AutoValue_SourceElement(element, Optional.of(contributedBy));
  }

  static final Function<SourceElement, Set<TypeElement>> CONTRIBUTING_CLASS =
      new Function<SourceElement, Set<TypeElement>>() {
        @Override
        public Set<TypeElement> apply(SourceElement sourceElement) {
          return sourceElement.contributedBy().asSet();
        }
      };
      
  static Predicate<SourceElement> hasModifier(final Modifier modifier) {
    return new Predicate<SourceElement>() {
      @Override
      public boolean apply(SourceElement sourceElement) {
        return sourceElement.element().getModifiers().contains(modifier);
      }
    };
  }
}
