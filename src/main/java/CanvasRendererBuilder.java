/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.rinde.rinsim.core.model.ModelProvider;

/**
 * Builder interface for creating {@link CanvasRenderer}s.
 * @author Rinde van Lon
 */
public interface CanvasRendererBuilder {
  /**
   * Should construct a {@link CanvasRenderer}.
   * @param mp A {@link ModelProvider} that can be used to obtain model
   *          dependencies for the renderer.
   * @return A {@link CanvasRenderer} instance.
   */
  CanvasRenderer build(ModelProvider mp);

  /**
   * Should create a complete copy of the builder.
   * @return An instance with exactly the same behavior as this.
   */
  CanvasRendererBuilder copy();
}