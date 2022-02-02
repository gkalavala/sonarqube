/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.bootstrap;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.ComponentKeys;
import org.sonar.core.platform.Container;
import org.sonar.core.platform.ExtensionContainer;
import org.sonar.core.platform.PluginInfo;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class SpringComponentContainer implements ExtensionContainer {
  protected final AnnotationConfigApplicationContext context;
  @Nullable
  protected final SpringComponentContainer parent;

  private final PropertyDefinitions propertyDefinitions;
  private final ComponentKeys componentKeys = new ComponentKeys();

  protected SpringComponentContainer() {
    this(null, new PropertyDefinitions(System2.INSTANCE), emptyList());
  }

  protected SpringComponentContainer(List<?> externalExtensions) {
    this(null, new PropertyDefinitions(System2.INSTANCE), externalExtensions);
  }

  protected SpringComponentContainer(SpringComponentContainer parent) {
    this(parent, parent.propertyDefinitions, emptyList());
  }

  private SpringComponentContainer(@Nullable SpringComponentContainer parent, PropertyDefinitions propertyDefinitions, List<?> externalExtensions) {
    this.parent = parent;
    this.propertyDefinitions = propertyDefinitions;
    this.context = new AnnotationConfigApplicationContext(new PriorityBeanFactory());
    // it won't set the name of beans created with @Bean annotated methods
    this.context.setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
    if (parent != null) {
      context.setParent(parent.context);
    }
    add(this);
    add(new StartableBeanPostProcessor());
    add(externalExtensions);
    add(propertyDefinitions);
  }

  /**
   * Beans need to have a unique name, otherwise they'll override each other.
   * The strategy is:
   * - For classes, use the fully qualified class name as the name of the bean
   * - For instances, use the FQCN + toString()
   * - If the object is a collection, iterate through the elements and apply the same strategy for each of them
   */
  @Override
  public Container add(Object... objects) {
    for (Object o : objects) {
      if (o instanceof Class) {
        Class<?> clazz = (Class<?>) o;
        context.registerBean(clazz);
      } else if (o instanceof Iterable) {
        add(Iterables.toArray((Iterable<?>) o, Object.class));
      } else {
        registerInstance(o);
      }
    }
    return this;
  }

  private <T> void registerInstance(T instance) {
    Supplier<T> supplier = () -> instance;
    Class<T> clazz = (Class<T>) instance.getClass();
    context.registerBean(componentKeys.ofInstance(instance), clazz, supplier);
    declareExtension("", instance);
  }

  /**
   * Extensions are usually added by plugins and we assume they don't support any injection-related annotations.
   * Spring contexts supporting annotations will fail if multiple constructors are present without any annotations indicating which one to use for injection.
   * For that reason, we need to create the beans ourselves, using ClassDerivedBeanDefinition, which will declare that all constructors can be used for injection.
   */
  private Container addExtension(Object o) {
    if (o instanceof Class) {
      Class<?> clazz = (Class<?>) o;
      ClassDerivedBeanDefinition bd = new ClassDerivedBeanDefinition(clazz);
      context.registerBeanDefinition(clazz.getName(), bd);
    } else if (o instanceof Iterable) {
      ((Iterable<?>) o).forEach(this::addExtension);
    } else {
      ClassDerivedBeanDefinition bd = new ClassDerivedBeanDefinition(o.getClass());
      bd.setInstanceSupplier(() -> o);
      context.registerBeanDefinition(componentKeys.ofInstance(o), bd);
    }
    return this;
  }

  @Override
  public Container addSingletons(Iterable<?> components) {
    return add(components);
  }

  @Override
  public <T> T getComponentByType(Class<T> type) {
    try {
      return context.getBean(type);
    } catch (Exception t) {
      throw new IllegalStateException("Unable to load component " + type, t);
    }
  }

  @Override
  public <T> List<T> getComponentsByType(Class<T> type) {
    try {
      return new ArrayList<>(context.getBeansOfType(type).values());
    } catch (Exception t) {
      throw new IllegalStateException("Unable to load components " + type, t);
    }
  }

  public void execute() {
    RuntimeException r = null;
    try {
      startComponents();
    } catch (RuntimeException e) {
      r = e;
    } finally {
      try {
        stopComponents();
      } catch (RuntimeException e) {
        if (r == null) {
          r = e;
        }
      }
    }
    if (r != null) {
      throw r;
    }
  }

  public SpringComponentContainer startComponents() {
    doBeforeStart();
    context.refresh();
    doAfterStart();
    return this;
  }

  public SpringComponentContainer stopComponents() {
    if (context.isActive()) {
      context.close();
    }
    return this;
  }

  public SpringComponentContainer createChild() {
    return new SpringComponentContainer(this);
  }

  @Override
  @CheckForNull
  public SpringComponentContainer getParent() {
    return parent;
  }

  @Override
  public SpringComponentContainer addExtension(@Nullable PluginInfo pluginInfo, Object extension) {
    addExtension(extension);
    declareExtension(pluginInfo, extension);
    return this;
  }

  @Override
  public SpringComponentContainer addExtension(@Nullable String defaultCategory, Object extension) {
    addExtension(extension);
    declareExtension(defaultCategory, extension);
    return this;
  }

  @Override
  public SpringComponentContainer declareExtension(@Nullable PluginInfo pluginInfo, Object extension) {
    declareExtension(pluginInfo != null ? pluginInfo.getName() : "", extension);
    return this;
  }

  @Override
  public SpringComponentContainer declareExtension(@Nullable String defaultCategory, Object extension) {
    this.propertyDefinitions.addComponent(extension, ofNullable(defaultCategory).orElse(""));
    return this;
  }

  /**
   * This method aims to be overridden
   */
  protected void doBeforeStart() {
    // nothing
  }

  /**
   * This method aims to be overridden
   */
  protected void doAfterStart() {
    // nothing
  }
}