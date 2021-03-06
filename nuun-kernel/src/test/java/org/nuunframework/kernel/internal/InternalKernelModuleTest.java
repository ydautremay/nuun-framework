package org.nuunframework.kernel.internal;

import java.util.HashMap;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuunframework.kernel.context.InitContextInternal;
import org.nuunframework.kernel.stereotype.ConcernTest;
import org.nuunframework.kernel.stereotype.sample.CachePlugin;
import org.nuunframework.kernel.stereotype.sample.LogPlugin;
import org.nuunframework.kernel.stereotype.sample.SecurityPlugin;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.StaticInjectionRequest;

public class InternalKernelModuleTest
{

    Injector injector;
	private InternalKernelGuiceModule underTest;

    @Before
    public void init()
    {

        underTest = new InternalKernelGuiceModule(new InitContextInternal("nuun-", new HashMap<String, String>()) );
//        Module aggregationModule = new AbstractModule()
//        {
//
//            @Override
//            protected void configure()
//            {
//                bind(Holder.class);
//                bind(HolderForPlugin.class);
//                bind(HolderForContext.class);
//                bind(HolderForPrefixWithName.class);
//                bind(HolderForBeanWithParentType.class);
//                install(underTest);
//            }
//        };
//
//        injector = Guice.createInjector(Stage.PRODUCTION, aggregationModule);
    }

    // @Test
    // public void logger_should_be_injected_even_on_child_class()
    // {
    // Holder holder = injector.getInstance(HolderChild.class);
    // assertThat(holder.getLogger()).isNotNull();
    // }
    //

    
    @Test
    public void computeOrder_should_works ()
    {
    	Assertions.assertThat( underTest.computeOrder(SecurityPlugin.Module.class)).isEqualTo(2000);
    	Assertions.assertThat( underTest.computeOrder(LogPlugin.Module.class)).isEqualTo(-20);
    	Assertions.assertThat( underTest.computeOrder(CachePlugin.Module.class)).isEqualTo(1998);
    	Assertions.assertThat( underTest.computeOrder(ConcernTest.Module.class)).isEqualTo(Integer.MIN_VALUE);
    }
    
    @Test
    @Ignore
    public void injectorCheck()
    {
        for (Key<?> key : injector.getAllBindings().keySet())
        {
            System.err.println("> " + key + " => " + key.getTypeLiteral().getRawType());
            Binding<?> binding = injector.getBinding(key);
            binding.acceptVisitor(new DefaultElementVisitor<Void>()
            {

                /*
                 * (non-Javadoc)
                 * 
                 * @see
                 * com.google.inject.spi.DefaultElementVisitor#visit(com.google
                 * .inject.Binding)
                 */
                @Override
                public <T> Void visit(Binding<T> binding)
                {
                    System.err.println(">> Binding " + binding.toString().replaceAll(",", ",\n   ") + " at \n   " + binding.getSource());
                    return null;

                }

                @Override
                public Void visit(StaticInjectionRequest element)
                {
                    System.err.println(">> Static injection is fragile! Please fix " + element.getType().getName() + " at " + element.getSource());
                    return null;
                }
            });

        }
    }

}
