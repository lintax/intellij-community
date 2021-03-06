/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInspection.ex

import com.intellij.codeInspection.InspectionProfile
import com.intellij.configurationStore.SerializableScheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PathMacroManager
import com.intellij.profile.ProfileEx
import com.intellij.profile.codeInspection.BaseInspectionProfileManager
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.util.xmlb.annotations.Transient

abstract class NewInspectionProfile(name: String, private var profileManager: BaseInspectionProfileManager) : ProfileEx(name), InspectionProfile, SerializableScheme {
  private var isProjectLevel: Boolean = false

  @Transient
  fun isProjectLevel() = isProjectLevel

  fun setProjectLevel(value: Boolean) {
    isProjectLevel = value
  }

  @Transient
  fun getProfileManager() = profileManager

  fun setProfileManager(value: BaseInspectionProfileManager) {
    profileManager = value
  }

  protected val pathMacroManager: PathMacroManager
    get() {
      val profileManager = profileManager
      return PathMacroManager.getInstance((profileManager as? ProjectInspectionProfileManager)?.project ?: ApplicationManager.getApplication())
    }
}