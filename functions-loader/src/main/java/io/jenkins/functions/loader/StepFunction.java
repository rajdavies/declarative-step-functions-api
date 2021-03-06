/**
 * Copyright (C) Original Authors 2017
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jenkins.functions.loader;

import java.util.Map;

/**
 * Java interface for working with Java step function implementations
 */
public interface StepFunction {
    /**
     * Invoke the step function passing in the given arguments
     *
     * @param arguments optional arguments by name
     * @return the result of the step function
     */
    Object invoke(Map<String, Object> arguments);

    /**
     * Returns the metadata for the step function
     */
    StepMetadata getMetadata();
}
