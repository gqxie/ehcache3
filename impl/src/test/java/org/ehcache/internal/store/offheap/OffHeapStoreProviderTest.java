/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.internal.store.offheap;

import org.ehcache.config.Eviction;
import org.ehcache.config.EvictionPrioritizer;
import org.ehcache.config.EvictionVeto;
import org.ehcache.config.ResourcePool;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.ResourceType;
import org.ehcache.config.ResourceUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Expirations;
import org.ehcache.expiry.Expiry;
import org.ehcache.spi.ServiceLocator;
import org.ehcache.spi.cache.Store;
import org.ehcache.spi.serialization.SerializationProvider;
import org.junit.Test;
import org.terracotta.context.ContextManager;
import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Matcher;
import org.terracotta.context.query.Matchers;
import org.terracotta.context.query.Query;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.terracotta.context.query.Matchers.attributes;
import static org.terracotta.context.query.Matchers.context;
import static org.terracotta.context.query.Matchers.hasAttribute;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;

/**
 * OffHeapStoreProviderTest
 */
public class OffHeapStoreProviderTest {
  @Test
   public void testStatisticsAssociations() throws Exception {
     OffHeapStore.Provider provider = new OffHeapStore.Provider();

    ServiceLocator serviceLocator = new ServiceLocator(mock(SerializationProvider.class));

    provider.start(null, serviceLocator);

    OffHeapStore<Long, String> store = provider.createStore(getStoreConfig());

     Query storeQuery = queryBuilder()
         .children()
         .filter(context(attributes(Matchers.<Map<String, Object>>allOf(
             hasAttribute("tags", new Matcher<Set<String>>() {
               @Override
               protected boolean matchesSafely(Set<String> object) {
                 return object.containsAll(singleton("store"));
               }
             })))))
         .build();

     Set<TreeNode> nodes = singleton(ContextManager.nodeFor(store));

     Set<TreeNode> storeResult = storeQuery.execute(nodes);
     assertThat(storeResult.isEmpty(), is(false));

     provider.releaseStore(store);

     storeResult = storeQuery.execute(nodes);
     assertThat(storeResult.isEmpty(), is(true));
   }

   private Store.Configuration<Long, String> getStoreConfig() {
     return new Store.Configuration<Long, String>() {
       @Override
       public Class<Long> getKeyType() {
         return Long.class;
       }

       @Override
       public Class<String> getValueType() {
         return String.class;
       }

       @Override
       public EvictionVeto<? super Long, ? super String> getEvictionVeto() {
         return Eviction.none();
       }

       @Override
       public EvictionPrioritizer<? super Long, ? super String> getEvictionPrioritizer() {
         return Eviction.Prioritizer.LRU;
       }

       @Override
       public ClassLoader getClassLoader() {
         return getClass().getClassLoader();
       }

       @Override
       public Expiry<? super Long, ? super String> getExpiry() {
         return Expirations.noExpiration();
       }

       @Override
       public ResourcePools getResourcePools() {
         return new ResourcePools() {
           @Override
           public ResourcePool getPoolForResource(ResourceType resourceType) {
             return new ResourcePool() {
               @Override
               public ResourceType getType() {
                 return ResourceType.Core.OFFHEAP;
               }

               @Override
               public long getSize() {
                 return 10;
               }

               @Override
               public ResourceUnit getUnit() {
                 return MemoryUnit.MB;
               }

               @Override
               public boolean isPersistent() {
                 return false;
               }
             };
           }

           @Override
           public Set<ResourceType> getResourceTypeSet() {
             return singleton((ResourceType) ResourceType.Core.OFFHEAP);
           }
         };
       }

     };
   }}
