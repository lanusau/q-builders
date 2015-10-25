package com.github.rutledgepaulv.basic.qbuilders.builders;

import com.github.rutledgepaulv.basic.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.basic.qbuilders.conditions.Partial;
import com.github.rutledgepaulv.basic.qbuilders.delegates.concrete.*;
import com.github.rutledgepaulv.basic.qbuilders.delegates.virtual.Delegate;
import com.github.rutledgepaulv.basic.qbuilders.delegates.virtual.PropertyDelegate;
import com.github.rutledgepaulv.basic.qbuilders.nodes.*;
import com.github.rutledgepaulv.basic.qbuilders.operators.basic.ComparisonOperator;
import com.github.rutledgepaulv.basic.qbuilders.properties.concrete.basic.*;
import com.github.rutledgepaulv.basic.qbuilders.properties.virtual.Property;
import com.github.rutledgepaulv.basic.qbuilders.utilities.ObjectUtils;
import com.github.rutledgepaulv.basic.qbuilders.utilities.VarArgUtils;
import com.github.rutledgepaulv.basic.qbuilders.visitors.NodeVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class QBuilder<T extends QBuilder<T>> implements Partial<T> {

    private LogicalNode root;
    private LogicalNode current;

    public QBuilder() {
        root = new OrNode(null, new ArrayList<>());
        current = root;
    }

    public final BooleanProperty<T> bool(String field) {
        return prop(field, BooleanPropertyDelegate.class, BooleanProperty.class);
    }

    public final StringProperty<T> string(String field) {
        return prop(field, StringPropertyDelegate.class, StringProperty.class);
    }

    public final ShortProperty<T> shortNum(String field) {
        return prop(field, ShortPropertyDelegate.class, ShortProperty.class);
    }

    public final IntegerProperty<T> intNum(String field) {
        return prop(field, IntegerPropertyDelegate.class, IntegerProperty.class);
    }

    public final LongProperty<T> longNum(String field) {
        return prop(field, LongPropertyDelegate.class, LongProperty.class);
    }

    public final FloatProperty<T> floatNum(String field) {
        return prop(field, FloatPropertyDelegate.class, FloatProperty.class);
    }

    public final DoubleProperty<T> doubleNum(String field) {
        return prop(field, DoublePropertyDelegate.class, DoubleProperty.class);
    }

    public final <S extends PropertyDelegate<T>, Q extends Property<T>> Q prop(String field, Class<S> delegate, Class<Q> inter) {
        if(!inter.isAssignableFrom(delegate)) {
            throw new IllegalArgumentException("Must provide a delegate that implements the interface to be returned.");
        }

        return (Q) ObjectUtils.init(delegate, field, self());
    }

    @SafeVarargs
    public final Condition<T> and(Condition<T> c1, Condition<T> c2, Condition<T>... cn) {
        return and(VarArgUtils.combine(c1, c2, cn));
    }

    @SafeVarargs
    public final Condition<T> or(Condition<T> c1, Condition<T> c2, Condition<T>... cn) {
        return or(VarArgUtils.combine(c1, c2, cn));
    }

    public final Condition<T> and(List<Condition<T>> conditions) {
        return combine(conditions, AndNode.class);
    }

    public final Condition<T> or(List<Condition<T>> conditions) {
        return combine(conditions, OrNode.class);
    }


    private <S extends LogicalNode> Condition<T> combine(List<Condition<T>> conditions, Class<S> type) {

        List<AbstractNode> children = conditions.stream()
                .map(condition -> ((QBuilder<T>)((QBuilder<T>) condition).self()).current)
                .collect(Collectors.toList());

        S node = ObjectUtils.init(type, ((QBuilder<T>)self()).current, children);
        ((QBuilder<T>)self()).current.getChildren().add(node);

        return new ConditionDelegate(self());
    }

    protected final Condition<T> condition(String field, ComparisonOperator operator, Collection<?> values) {
        ComparisonNode node = new ComparisonNode(((QBuilder<T>)self()).current);

        node.setField(field);
        node.setOperator(operator);
        node.setValues(values);

        ((QBuilder<T>)self()).current.getChildren().add(node);
        return new ConditionDelegate(self());
    }

    protected T self() {
        return (T) this;
    }

    private final class ConditionDelegate extends Delegate<T> implements Condition<T> {

        private ConditionDelegate(T canonical) {
            super(canonical);
        }

        public final T and() {
            QBuilder<T> self = self();
            LogicalNode current = self.current;
            List<AbstractNode> children = new ArrayList<>();
            children.add(current);
            AndNode node = new AndNode(current.getParent(), children);
            if (current == self.root) {
                self.root = node;
            }
            self.current = node;
            return (T) self;
        }

        public final T or() {
            QBuilder<T> self = self();
            LogicalNode current = self.current;
            List<AbstractNode> children = new ArrayList<>();
            children.add(current);
            OrNode node = new OrNode(current.getParent(), children);
            if (current == self.root) {
                self.root = node;
            }
            self.current = node;
            return (T) self;
        }

        public final <Q> Q query(NodeVisitor<Q> visitor) {
            QBuilder<T> self = self();
            return self.root.visit(visitor);
        }

    }


}
