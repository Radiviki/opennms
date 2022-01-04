/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.config.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.opennms.features.config.exception.ConfigConversionException;

public class ConfigConvertUtil {
    // use object pool to prevent global locking issue
    private static final ObjectPool<ObjectMapper> pool = new GenericObjectPool<>(new ObjectMapperFactory());

    static class ObjectMapperFactory extends BasePoolableObjectFactory<ObjectMapper> {

        @Override
        public ObjectMapper makeObject() {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.registerModule(new Jdk8Module())
                    .setPropertyNamingStrategy(new PropertyNamingStrategies.KebabCaseStrategy());
        }
    }

    private ConfigConvertUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Convert config json to entity class
     *
     * @param jsonStr
     * @param entityClass
     * @param <E>         entity class
     * @return
     */
    public static <E> E jsonToObject(String jsonStr, Class<E> entityClass) {
        ObjectMapper mapper;
        try {
            mapper = pool.borrowObject();
        } catch (Exception e) {
            throw new ConfigConversionException("Fail to borrow ObjectMapper. ", e);
        }
        try {
            return mapper.readValue(jsonStr, entityClass);
        } catch (JsonProcessingException e) {
            throw new ConfigConversionException("Fail to convert json to object. ", e);
        } finally {
            try {
                pool.returnObject(mapper);
            } catch (Exception e) {
                throw new ConfigConversionException("Fail to return ObjectMapper. ", e);
            }
        }
    }

    public static String objectToJson(Object object) {
        ObjectMapper mapper;
        try {
            mapper = pool.borrowObject();
        } catch (Exception e) {
            throw new ConfigConversionException("Fail to borrow ObjectMapper. ", e);
        }
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ConfigConversionException("Fail to convert object to json. ", e);
        } finally {
            try {
                pool.returnObject(mapper);
            } catch (Exception e) {
                throw new ConfigConversionException("Fail to return ObjectMapper. ", e);
            }
        }
    }
}