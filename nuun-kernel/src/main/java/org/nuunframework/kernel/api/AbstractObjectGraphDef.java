package org.nuunframework.kernel.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.nuunframework.kernel.api.grapher.Grapher;
import org.nuunframework.kernel.api.grapher.GrapherInternal;
import org.nuunframework.kernel.api.grapher.InstanceBuilder;
import org.nuunframework.kernel.api.grapher.ReferenceBuilder;
import org.nuunframework.kernel.commons.specification.Specification;

public abstract class AbstractObjectGraphDef implements ObjectGraphDef 
{
    protected Grapher grapher;
    private GrapherInternal internalGrapher;

    @Override
    public ObjectGraph generate()
    {
        internalGrapher = new GrapherInternal();
        grapher = internalGrapher;
        List<Instance> instances = new ArrayList<Instance>();
        List<Reference> references = new ArrayList<Reference>();
        Map<String, Instance> mapInstances = new HashMap<String, Instance>();
        Map<String, Reference> mapReferences = new HashMap<String, Reference>();
        Map<Class<?>, List<Instance>> mapInstancesByClasses = new HashMap<Class<?>, List<Instance>>();
        try
        {
            describe();
            for( org.nuunframework.kernel.api.grapher.GrapherInternal.instance instance : this.internalGrapher.instances)
            {
                InstanceInternal i = new InstanceInternal(instance.name, instance.type, new ArrayList<Reference>());
                i.meta(instance.metadata);
                instances.add(i);
                mapInstances.put(i.name, i);
                List<Instance> list = mapInstancesByClasses.get(i.type);
                if (list == null )
                {
                    list = new ArrayList<Instance>();
                    mapInstancesByClasses.put(i.type, list);
                }
                list.add(i);
            }
            
            for (org.nuunframework.kernel.api.grapher.GrapherInternal.reference reference : this.internalGrapher.references)
            {
                InstanceInternal source = (InstanceInternal) mapInstances.get(reference.source);
                InstanceInternal target = (InstanceInternal) mapInstances.get(reference.target);
                checkNotNull(source, "source");
                checkNotNull(target, "target");
                ReferenceInternal r = new ReferenceInternal(reference.name, source, target);
                r.optionnal(reference.optionnal);
                references.add(r);
                mapReferences.put(r.name, r);
                source.references.add(r);
            }
            
        }
        finally
        {
            grapher = internalGrapher = null;
        }
        return new ObjectGraphInternal(instances, references, mapInstances, mapReferences, mapInstancesByClasses);
    }
    
    static class ObjectGraphInternal implements ObjectGraph
    {

        private List<Instance> instances;
        private List<Reference> references;
        private Map<String, Instance> mapInstances;
        private Map<String, Reference> mapReferences;
        private Map<Class<?>, List<Instance>> mapInstancesByClasses;

        ObjectGraphInternal( 
                List<Instance> instances , 
                List<Reference> references ,
                Map<String, Instance> mapInstances ,
                Map<String, Reference> mapReferences ,
                Map<Class<?>, List<Instance>> mapInstancesByClasses
                )
        {
            this.instances = instances;
            this.references = references;
            this.mapInstances = mapInstances;
            this.mapReferences = mapReferences;
            this.mapInstancesByClasses = mapInstancesByClasses;
        }
        
        @Override
        public Collection<Instance> instances()
        {
            return Collections.unmodifiableCollection(this.instances);
        }

        @Override
        public Collection<Reference> references()
        {
            return Collections.unmodifiableCollection(this.references);
        }
        

        @Override
        public Collection<Instance> instancesAsWildcard()
        {
            Collection<Instance> match = new HashSet<Instance>();
            for(Instance instance : instances)
            {
                if ( instance.meta() == InstanceMeta.WILDCARD) 
                {
                    match.add(instance);
                }
            }
            return match;

        }
        @Override
        public Collection<Instance> instancesAssignableFrom(Class<?> klass)
        {
            Collection<Instance> match = new HashSet<Instance>();
            for(Instance instance : instances)
            {
                if ( klass.isAssignableFrom(instance.type()) ) 
                {
                    match.add(instance);
                }
            }
            return match;
        }
        @Override
        public Collection<Instance> instancesByClass(Class<?> klass)
        {
            List<Instance> list = mapInstancesByClasses.get(klass);
            if (list != null)
            {
                return Collections.unmodifiableCollection(list);
            }
            else
            {
                return Collections.emptySet();
            }
        }
        
        @Override
        public Collection<Reference> referencesTargetAssignableFrom(Class<?> klass)
        {
            Collection<Reference> match = new HashSet<Reference>();
            for(Reference reference : references)
            {
                if ( klass.isAssignableFrom(reference.target().type())) 
                    match.add(reference);
            }
            return match;
        }
        @Override
        public Collection<Reference> referencesByTargetType(Class<?> klass)
        {
            Collection<Reference> match = new HashSet<Reference>();
            for(Reference reference : references)
            {
                if (reference.target().type().equals(klass)) 
                    match.add(reference);
            }
            return match;
        }
        
        
        @Override
        public Collection<Reference> referencesByRegex(String regex)
        {
            Collection<Reference> match = new HashSet<Reference>();
            for(Reference reference : references)
            {
                if (reference.name().matches(regex)) match.add(reference);
            }
            return match;
        }

        @Override
        public Collection<Instance> instancesByRegex(String regex)
        {
            Collection<Instance> match = new HashSet<Instance>();
            for(Instance instance : instances)
            {
                if (instance.name().matches(regex)) match.add(instance);
            }
            return match;
        }
        @Override
        public Instance instance(String name)
        {
            return mapInstances.get(name);
        }
        @Override
        public Reference reference(String name)
        {
            return mapReferences.get(name);
        }
    }
    
    protected abstract void describe();
    
    protected InstanceBuilder newInstance (String name , Class<?> type)
    {
        return internalGrapher.newInstance(name, type);
    }
    
    protected ReferenceBuilder newReference(String referenceName)
    {
        return internalGrapher.newReference(referenceName);
    }
    protected ReferenceBuilder newReference()
    {
        return internalGrapher.newReference(UUID.randomUUID().toString());
    }
    
    private static class ReferenceInternal implements Reference
    {
        private String name;
        private Instance source;
        private Instance target;
        private boolean optionnal;


        public ReferenceInternal(String name , Instance source, Instance target)
        {
            this.name = name;
            this.source = source;
            this.target = target;
            this.optionnal = false;
        }

        @Override
        public String name()
        {
            return this.name;
        }

        @Override
        public Instance source()
        {
            return this.source;
        }

        @Override
        public Instance target()
        {
            return this.target;
        }

        @Override
        public boolean optionnal()
        {
            return optionnal;
        }

        
        public void optionnal(boolean inOptionnal)
        {
            this.optionnal = inOptionnal;
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((source == null) ? 0 : source.hashCode());
            result = prime * result + ((target == null) ? 0 : target.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ReferenceInternal other = (ReferenceInternal) obj;
            if (name == null)
            {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            if (source == null)
            {
                if (other.source != null)
                    return false;
            }
            else if (!source.equals(other.source))
                return false;
            if (target == null)
            {
                if (other.target != null)
                    return false;
            }
            else if (!target.equals(other.target))
                return false;
            return true;
        }
        
        
        
    }
    
    private static class InstanceInternal implements Instance
    {

        private String name;
        private Class<?> type;
        private List<Reference> references; 
        private InstanceMeta meta = InstanceMeta.NOMINAL;
        private Specification<Class<?>> typeSubset;
        
        InstanceInternal(String name , Class<?> type , List<Reference> references)
        {
            this.type = type;
            this.name = name;
            this.references = references;
        }
        
        
        @Override
        public String name()
        {
            return name;
        }

        @Override
        public Class<?> type()
        {
            return type;
        }
        
        @Override
        public InstanceMeta meta()
        {
            return this.meta;
        }
        
        public Instance meta(InstanceMeta meta)
        {
            this.meta = meta;
            return this;
        }


        @Override
        public Collection<Reference> references()
        {
            return Collections.unmodifiableCollection(this.references);
        }
        
        @Override
        public Specification<Class<?>> typeSubset()
        {
            return this.typeSubset;
        }

        public void typeSubset(Specification<Class<?>> typeSubset)
        {
            this.typeSubset = typeSubset;
        }
        
        @Override
        public Collection<Reference> referencesByRegex(String regex)
        {
            Collection<Reference> match = new HashSet<Reference>();
            for(Reference reference : references)
            {
                if (reference.name().matches(regex)) match.add(reference);
            }
            return match;
        }

        
        @Override
        public Collection<Reference> referencesAssignableFrom(Class<?> klass)
        {
            Collection<Reference> match = new HashSet<Reference>();
            for(Reference reference : this.references)
            {
                if ( klass.isAssignableFrom(reference.target().type()) ) 
                {
                    match.add(reference);
                }
            }
            return match;
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }


        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            InstanceInternal other = (InstanceInternal) obj;
            if (name == null)
            {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            if (type == null)
            {
                if (other.type != null)
                    return false;
            }
            else if (!type.equals(other.type))
                return false;
            return true;
        }


     
        
    }
    

}
