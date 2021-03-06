package org.nuunframework.kernel.context;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.nuunframework.kernel.annotations.KernelModule;
import org.nuunframework.kernel.commons.specification.Specification;
import org.nuunframework.kernel.commons.specification.reflect.DescendantOfSpecification;
import org.nuunframework.kernel.internal.scanner.ClasspathScanner;
import org.nuunframework.kernel.internal.scanner.ClasspathScannerFactory;
import org.nuunframework.kernel.plugin.Plugin;
import org.nuunframework.kernel.plugin.request.RequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.inject.Module;
import com.google.inject.Scopes;
@SuppressWarnings("rawtypes")
public class InitContextInternal implements InitContext
{

    private Logger                                                       logger = LoggerFactory.getLogger(InitContextInternal.class);

    ClasspathScanner                                                     classpathScanner;

    private final List<Class<?>>                                         parentTypesClassesToScan;
    private final List<Class<?>>                                         ancestorTypesClassesToScan;
    private final List<Class<?>>                                         typesClassesToScan;
    private final List<String>                                           typesRegexToScan;
    private final List<Specification<Class<?>>>                          specificationsToScan;
    private final List<String>                                           resourcesRegexToScan;
    private final List<String>                                           parentTypesRegexToScan;
    private final List<Class<? extends Annotation>>                      annotationTypesToScan;
    private final List<String>                                           annotationRegexToScan;

    private final List<Class<?>>                                         parentTypesClassesToBind;
    private final List<Class<?>>                                         ancestorTypesClassesToBind;
    private final List<String>                                           parentTypesRegexToBind;
    private final List<Specification<Class<?>>>                          specificationsToBind;
    private final Map<Key , Object>                                       mapOfScopes;
    private final List<Class<? extends Annotation>>                      annotationTypesToBind;
    private final List<Class<? extends Annotation>>                      metaAnnotationTypesToBind;
    private final List<String>                                           annotationRegexToBind;
    private final List<String>                                           metaAnnotationRegexToBind;

    private final List<String>                                           propertiesPrefix;

    private final List<Module>                                           childModules;
    private final List<String>                                           packageRoots;
    private final Set<URL>                                               additionalClasspathScan;

    private List<Class<?>>                                               classesToBind;
    private Map<Class<?> , Object>                                       classesWithScopes;

    private Collection<String>                                           propertiesFiles;

    private final Map<String, String>                                    kernelParams;

    private Collection<Class<?>>                                         scanClasspathForSubType;
    private Collection<Class<?>>                                         scanClasspathForAncestorType;
    private Collection<Class<?>>                                         bindClasspathForSubType;
    private Collection<Class<?>>                                         bindClasspathForAncestorType;
    private final Map<Class<?>, Collection<Class<?>>>                    mapSubTypes;
    private final Map<Class<?>, Collection<Class<?>>>                    mapAncestorTypes;
    // private final Map<Class<?> , Collection<Class<?>>> mapTypes;
    private final Map<String, Collection<Class<?>>>                      mapSubTypesByName;
    private final Map<String, Collection<Class<?>>>                      mapTypesByName;
    private final Map<Specification, Collection<Class<?>>>               mapTypesBySpecification;
    private final Map<Class<? extends Annotation>, Collection<Class<?>>> mapAnnotationTypes;
    private final Map<String, Collection<Class<?>>>                      mapAnnotationTypesByName;
    private final Map<String, Collection<String>>                        mapPropertiesFiles;
    private final Map<String, Collection<String>>                        mapResourcesByRegex;

    /**
     * @param inPackageRoots
     */
    public InitContextInternal(String initialPropertiesPrefix, Map<String, String> kernelParams)
    {
        this.kernelParams = kernelParams;
        this.mapSubTypes = new HashMap<Class<?>, Collection<Class<?>>>();
        this.mapAncestorTypes = new HashMap<Class<?>, Collection<Class<?>>>();
        // this.mapTypes = new HashMap<Class<?>, Collection<Class<?>>>();
        this.mapSubTypesByName = new HashMap<String, Collection<Class<?>>>();
        this.mapTypesByName = new HashMap<String, Collection<Class<?>>>();
        this.mapTypesBySpecification = new HashMap<Specification, Collection<Class<?>>>();
        this.mapAnnotationTypes = new HashMap<Class<? extends Annotation>, Collection<Class<?>>>();
        this.mapAnnotationTypesByName = new HashMap<String, Collection<Class<?>>>();
        this.mapPropertiesFiles = new HashMap<String, Collection<String>>();
        this.mapResourcesByRegex = new HashMap<String, Collection<String>>();

        this.annotationTypesToScan = new LinkedList<Class<? extends Annotation>>();
        this.parentTypesClassesToScan = new LinkedList<Class<?>>();
        this.ancestorTypesClassesToScan = new LinkedList<Class<?>>();
        this.typesClassesToScan = new LinkedList<Class<?>>();
        this.typesRegexToScan = new LinkedList<String>();
        this.specificationsToScan = new LinkedList<Specification<Class<?>>>(); 
        this.resourcesRegexToScan = new LinkedList<String>();
        this.parentTypesRegexToScan = new LinkedList<String>();
        this.annotationRegexToScan = new LinkedList<String>();

        this.annotationTypesToBind = new LinkedList<Class<? extends Annotation>>();
        this.metaAnnotationTypesToBind = new LinkedList<Class<? extends Annotation>>();
        this.parentTypesClassesToBind = new LinkedList<Class<?>>();
        this.ancestorTypesClassesToBind = new LinkedList<Class<?>>();
        this.parentTypesRegexToBind = new LinkedList<String>();
        this.specificationsToBind = new LinkedList<Specification<Class<?>>>();
        this.mapOfScopes = new HashMap<Key, Object>();
        this.annotationRegexToBind = new LinkedList<String>();
        this.metaAnnotationRegexToBind = new LinkedList<String>();

        this.propertiesPrefix = new LinkedList<String>();
        this.childModules = new LinkedList<Module>();
        this.propertiesPrefix.add(initialPropertiesPrefix);
        this.packageRoots = new LinkedList<String>();
        this.additionalClasspathScan = new HashSet<URL>();
        // for (String packageRoot : inPackageRoots)
        // {
        // this.packageRoots.add(packageRoot);
        // }
    }

    private void initScanner()
    {
        String[] rawArrays = new String[this.packageRoots.size()];
        this.packageRoots.toArray(rawArrays);
        this.classpathScanner = new ClasspathScannerFactory().create( this.additionalClasspathScan , rawArrays);
        
    }

    class Class2Instance implements Function<Class<? extends Module>, Module>
    {

        /*
         * (non-Javadoc)
         * @see com.google.common.base.Function#apply(java.lang.Object)
         */
        @Override
        public Module apply(Class<? extends Module> classpathClass)
        {
            try
            {
                return (Module) classpathClass.newInstance();
            }
            catch (InstantiationException e)
            {
                logger.warn("Error when instantiating module " + classpathClass, e);
            }
            catch (IllegalAccessException e)
            {
                logger.warn("Error when instantiating module " + classpathClass, e);
            }
            return null;
        }
    }

    
    public void executeRequests()
    {
        initScanner();

        classesToBind = new LinkedList<Class<?>>();
        classesWithScopes = new HashMap<Class<?>, Object>();
        
        { // bind modules
            Collection<Class<?>> scanClasspathForModules = this.classpathScanner.scanClasspathForAnnotation(KernelModule.class);
            @SuppressWarnings("unchecked")
            Collection<Module> modules = Collections2.transform((Collection) scanClasspathForModules, new Class2Instance());
            this.childModules.addAll(modules);
            // clässes.addAll(scanClasspathForModules);
        }

        // CLASSES TO SCAN
        for (Class<?> parentType : this.parentTypesClassesToScan)
        {
            scanClasspathForSubType = this.classpathScanner.scanClasspathForSubTypeClass(parentType);
            // clässes.addAll(scanClasspathForSubType);
            this.mapSubTypes.put(parentType, scanClasspathForSubType);
        }
        for (Class<?> parentType : this.ancestorTypesClassesToScan)
        {
            scanClasspathForAncestorType = this.classpathScanner.scanClasspathForSpecification(new DescendantOfSpecification(parentType));
            this.mapAncestorTypes.put(parentType, scanClasspathForAncestorType);
        }

        // for (Class<?> type : this.typesClassesToScan)
        // {
        // scanClasspathForSubType = this.classpathScanner.scanClasspathForTypeClass(type);
        // // clässes.addAll(scanClasspathForSubType);
        // this.mapTypes.put(type, scanClasspathForSubType);
        // }

        for (String typeName : this.parentTypesRegexToScan)
        {
            Collection<Class<?>> scanClasspathForTypeName = this.classpathScanner.scanClasspathForSubTypeRegex(typeName);
            // clässes.addAll(scanClasspathForTypeName);
            this.mapSubTypesByName.put(typeName, scanClasspathForTypeName);
        }
        for (String typeName : this.typesRegexToScan)
        {
            Collection<Class<?>> scanClasspathForTypeName = this.classpathScanner.scanClasspathForTypeRegex(typeName);
            // clässes.addAll(scanClasspathForTypeName);
            this.mapTypesByName.put(typeName, scanClasspathForTypeName);
        }
        for (Specification<Class<?>>  spec  : this.specificationsToScan)
        {
            Collection<Class<?>> scanClasspathForSpecification = this.classpathScanner.scanClasspathForSpecification(spec);
            mapTypesBySpecification.put( spec , scanClasspathForSpecification);
            
        }

        for (Class<? extends Annotation> annotationType : this.annotationTypesToScan)
        {
            Collection<Class<?>> scanClasspathForAnnotation = this.classpathScanner.scanClasspathForAnnotation(annotationType);
            // clässes.addAll(scanClasspathForAnnotation);
            this.mapAnnotationTypes.put(annotationType, scanClasspathForAnnotation);
        }

        for (String annotationName : this.annotationRegexToScan)
        {
            Collection<Class<?>> scanClasspathForAnnotation = this.classpathScanner.scanClasspathForAnnotationRegex(annotationName);
            // clässes.addAll(scanClasspathForAnnotation);
            this.mapAnnotationTypesByName.put(annotationName, scanClasspathForAnnotation);
        }

        // CLASSES TO BIND
        // ===============
        for (Class<?> parentType : this.parentTypesClassesToBind)
        {
            bindClasspathForSubType = this.classpathScanner.scanClasspathForSubTypeClass(parentType);
            
            RequestType requestType = RequestType.SUBTYPE_OF_BY_CLASS;
            addScopeToClasses( bindClasspathForSubType , scope(requestType , parentType ) , classesWithScopes);
            
            classesToBind.addAll(bindClasspathForSubType);
        }
        for (Class<?> ancestorType : this.ancestorTypesClassesToBind)
        {
            bindClasspathForAncestorType = this.classpathScanner.scanClasspathForSpecification(new DescendantOfSpecification(ancestorType));
            RequestType requestType = RequestType.SUBTYPE_OF_BY_TYPE_DEEP;
            addScopeToClasses( bindClasspathForAncestorType , scope(requestType , ancestorType ) , classesWithScopes);
            classesToBind.addAll(bindClasspathForAncestorType);
        }

        // TODO vérifier si ok parent types vs type. si ok changer de nom
        for (String typeName : this.parentTypesRegexToBind)
        {
            Collection<Class<?>> scanClasspathForTypeName = this.classpathScanner.scanClasspathForTypeRegex(typeName);
            RequestType requestType = RequestType.SUBTYPE_OF_BY_REGEX_MATCH;
            addScopeToClasses( scanClasspathForTypeName , scope(requestType , typeName ) , classesWithScopes);
            classesToBind.addAll(scanClasspathForTypeName);
        }

        for (Specification<Class<?>>  spec : this.specificationsToBind)
        {
            Collection<Class<?>> scanClasspathForTypeName = this.classpathScanner.scanClasspathForSpecification(spec);
            
            RequestType requestType = RequestType.VIA_SPECIFICATION;
            addScopeToClasses(scanClasspathForTypeName, scope(requestType , spec ) , classesWithScopes);
            
            classesToBind.addAll(scanClasspathForTypeName);
        }

        for (Class<? extends Annotation> annotationType : this.annotationTypesToBind)
        {
            Collection<Class<?>> scanClasspathForAnnotation = this.classpathScanner.scanClasspathForAnnotation(annotationType);
            RequestType requestType = RequestType.ANNOTATION_TYPE;
            addScopeToClasses( scanClasspathForAnnotation , scope(requestType , annotationType ) , classesWithScopes);
            classesToBind.addAll(scanClasspathForAnnotation);
        }

        for (Class<? extends Annotation> metaAnnotationType : this.metaAnnotationTypesToBind) 
        {
            Collection<Class<?>> scanClasspathForAnnotation = this.classpathScanner.scanClasspathForMetaAnnotation(metaAnnotationType);
            RequestType requestType = RequestType.META_ANNOTATION_TYPE;
            addScopeToClasses( scanClasspathForAnnotation , scope(requestType , metaAnnotationType ) , classesWithScopes);
            classesToBind.addAll(scanClasspathForAnnotation);
        }

        for (String annotationNameRegex : this.annotationRegexToBind)
        {
            Collection<Class<?>> scanClasspathForAnnotation = this.classpathScanner.scanClasspathForAnnotationRegex(annotationNameRegex);
            RequestType requestType = RequestType.ANNOTATION_REGEX_MATCH;
            addScopeToClasses( scanClasspathForAnnotation , scope(requestType , annotationNameRegex ) , classesWithScopes);
            classesToBind.addAll(scanClasspathForAnnotation);
        }

        for (String metaAnnotationNameRegex : this.metaAnnotationRegexToBind)
        {
            Collection<Class<?>> scanClasspathForAnnotation = this.classpathScanner.scanClasspathForMetaAnnotationRegex(metaAnnotationNameRegex);
            RequestType requestType = RequestType.META_ANNOTATION_REGEX_MATCH;
            addScopeToClasses( scanClasspathForAnnotation , scope(requestType , metaAnnotationNameRegex ) , classesWithScopes);
            classesToBind.addAll(scanClasspathForAnnotation);
        }

        // Resources to scan
        
        for (String regex : this.resourcesRegexToScan)
        {
            Collection<String> resourcesScanned = this.classpathScanner.scanClasspathForResource(regex);
            this.mapResourcesByRegex.put(regex, resourcesScanned);
        }
        
        // PROPERTIES TO FETCH
        propertiesFiles = new HashSet<String>();
        for (String prefix : this.propertiesPrefix)
        {
            Collection<String> propertiesFilesTmp = this.classpathScanner.scanClasspathForResource(prefix + ".*\\.properties");
            propertiesFiles.addAll(propertiesFilesTmp);
            this.mapPropertiesFiles.put(prefix, propertiesFilesTmp);
        }
    }

    private Object scope( RequestType requestType , Object spec)
    {
        Object scope = this.mapOfScopes.get( key( requestType ,  spec));
        if (null == scope) scope = Scopes.NO_SCOPE;
        return scope;
    }
    
    private void addScopeToClasses(Collection<Class<?>> classes , Object scope, Map<Class<?>, Object> inClassesWithScopes)
    {
        for (Class<?> klass : classes)
        {
            inClassesWithScopes.put(klass, scope);
        }
    }
    
    public void addClasspathsToScan(Set<URL> paths)
    {
        if (paths != null && paths.size() > 0)
        {
            this.additionalClasspathScan.addAll( paths );
        }
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> scannedSubTypesByParentClass()
    {
        return Collections.unmodifiableMap(this.mapSubTypes);
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> scannedSubTypesByAncestorClass()
    {
        return Collections.unmodifiableMap(this.mapAncestorTypes);
    }

    @Override
    public Map<String, Collection<Class<?>>> scannedSubTypesByParentRegex()
    {
        return Collections.unmodifiableMap(this.mapSubTypesByName);
    }

    @Override
    public Map<String, Collection<Class<?>>> scannedTypesByRegex()
    {
        return Collections.unmodifiableMap(this.mapTypesByName);
    }

    @Override
    public Map<Specification, Collection<Class<?>>> scannedTypesBySpecification()
    {
        return Collections.unmodifiableMap(this.mapTypesBySpecification);
    }

    @Override
    public Map<Class<? extends Annotation>, Collection<Class<?>>> scannedClassesByAnnotationClass()
    {
        return Collections.unmodifiableMap(this.mapAnnotationTypes);
    }

    @Override
    public Map<String, Collection<Class<?>>> scannedClassesByAnnotationRegex()
    {
        return Collections.unmodifiableMap(this.mapAnnotationTypesByName);
    }

    @Override
    public Map<String, Collection<String>> mapPropertiesFilesByPrefix()
    {
        return Collections.unmodifiableMap(this.mapPropertiesFiles);
    }
    
    @Override
    public Map<String, Collection<String>> mapResourcesByRegex()
    {
        return Collections.unmodifiableMap(this.mapResourcesByRegex);
    }

    public void addPropertiesPrefix(String prefix)
    {
        this.propertiesPrefix.add(prefix);
    }

    public void addPackageRoot(String root)
    {
        this.packageRoots.add(root);
    }

    public void addParentTypeClassToScan(Class<?> type)
    {
        this.parentTypesClassesToScan.add(type);
    }

    public void addAncestorTypeClassToScan(Class<?> type)
    {
        this.ancestorTypesClassesToScan.add(type);
    }

    public void addResourcesRegexToScan(String regex)
    {
        this.resourcesRegexToScan.add(regex);
    }

    public void addTypeClassToScan(Class<?> type)
    {
        this.typesClassesToScan.add(type);
    }

    private Key key(RequestType type , Object key)
    {
        return new Key(type, key);
    }
    
    public void addParentTypeClassToBind(Class<?> type , Object scope)
    {
        updateScope(key ( RequestType.SUBTYPE_OF_BY_CLASS ,  type), scope);
        this.parentTypesClassesToBind.add(type);
    }

    public void addAncestorTypeClassToBind(Class<?> type , Object scope)
    {
        updateScope(key ( RequestType.SUBTYPE_OF_BY_TYPE_DEEP ,  type), scope);
        this.ancestorTypesClassesToBind.add(type);
    }

    public void addTypeRegexesToScan(String type)
    {
        this.typesRegexToScan.add(type);
    }
    
    public void addSpecificationToScan(Specification<Class<?>> specification)
    {
        this.specificationsToScan.add(specification);
    }

    public void addParentTypeRegexesToScan(String type)
    {
        this.parentTypesRegexToScan.add(type);
    }

    public void addTypeRegexesToBind(String type , Object scope)
    {
        updateScope(key ( RequestType.TYPE_OF_BY_REGEX_MATCH,  type), scope);
        this.parentTypesRegexToBind.add(type);
    }
    /**
     * @category bind
     * @param specification
     */
//    public void addSpecificationToBind(Specification<Class<?>> specification)
//    {
//        this.specificationsToBind.add(specification);
//        this.mapSpecificationScope.put(specification, Scopes.NO_SCOPE);
//    }

    private void updateScope ( Key key , Object scope)
    {
        if (scope != null)
        {
            this.mapOfScopes.put(key, scope);
        }
        else
        {
            this.mapOfScopes.put(key, Scopes.NO_SCOPE);
        }
            
        
    }
    
    public void addSpecificationToBind(Specification<Class<?>> specification , Object scope)
    {
        this.specificationsToBind.add(specification);
        updateScope(key ( RequestType.VIA_SPECIFICATION ,  specification), scope);
    }

    public void addAnnotationTypesToScan(Class<? extends Annotation> types)
    {
        this.annotationTypesToScan.add(types);
    }

    public void addAnnotationTypesToBind(Class<? extends Annotation> types , Object scope)
    {
        this.annotationTypesToBind.add(types);
        updateScope(key ( RequestType.ANNOTATION_TYPE ,  types), scope);
    }

    public void addMetaAnnotationTypesToBind(Class<? extends Annotation> types , Object scope)
    {
        this.metaAnnotationTypesToBind.add(types);
        updateScope(key ( RequestType.META_ANNOTATION_TYPE ,  types), scope);
    }

    public void addAnnotationRegexesToScan(String names)
    {
        this.annotationRegexToScan.add(names);
    }

    public void addAnnotationRegexesToBind(String names, Object scope)
    {
        this.annotationRegexToBind.add(names);
        updateScope(key ( RequestType.ANNOTATION_REGEX_MATCH ,  names), scope);
    }
    public void addMetaAnnotationRegexesToBind(String names, Object scope)
    {
        this.metaAnnotationRegexToBind.add(names);
        updateScope(key ( RequestType.META_ANNOTATION_REGEX_MATCH,  names), scope);
    }

    public void addChildModule(Module module)
    {
        this.childModules.add(module);
    }

//    public void setContainerContext(Object containerContext)
//    {
//        this.containerContext = containerContext;
//    }

    // INTERFACE KERNEL PARAM USED BY PLUGIN IN INIT //

//    public Object containerContext()
//    {
//        return this.containerContext;
//    }

    @Override
    public String getKernelParam(String key)
    {
        return kernelParams.get(key);
    }

    @Override
    @SuppressWarnings({
            "unchecked", "rawtypes"
    })
    public List<Class<?>> classesToBind()
    {
        return (List) Collections.unmodifiableList(this.classesToBind);
    }
    
    @SuppressWarnings({"unchecked"})
    public Map<Class<?> , Object> classesWithScopes ()
    {
        return  (Map) Collections.unmodifiableMap(classesWithScopes );
    }

    @Override
    @SuppressWarnings({
            "unchecked"
    })
    public List<Module> moduleResults()
    {
        return (List) Collections.unmodifiableList(this.childModules);
    }

    @SuppressWarnings({
            "unchecked"
    })
    @Override
    public Collection<String> propertiesFiles()
    {
        return (Collection) Collections.unmodifiableCollection(this.propertiesFiles);
    }
    
    @Override
    public Collection<? extends Plugin> pluginsRequired()
    {
        return Collections.emptySet();
    }
    
    public Collection<? extends Plugin> dependentPlugins ()
    {
        return Collections.emptySet();
    }
    
    static class Key
    {
        private final RequestType type;
        private final Object key;
        
        public Key(RequestType type , Object key)
        {
            this.type = type;
            this.key = key;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            Key key2 = (Key)obj;
            return new EqualsBuilder().append(this.type, key2.type ).append( this.key, key2.key).isEquals() ;
        }
        
        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(this.type).append( this.key).toHashCode();
        }
    }

}