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
package com.intellij.vcs.log.ui.render;
/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.RefGroup;
import com.intellij.vcs.log.VcsLogRefManager;
import com.intellij.vcs.log.VcsRef;
import com.intellij.vcs.log.data.VcsLogData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

public class LabelPainter {
  public static final int TOP_TEXT_PADDING = JBUI.scale(1);
  public static final int BOTTOM_TEXT_PADDING = JBUI.scale(2);
  public static final int RIGHT_PADDING = JBUI.scale(2);
  public static final int LEFT_PADDING = JBUI.scale(2);
  public static final int MIDDLE_PADDING = JBUI.scale(2);
  private static final int MAX_LENGTH = 22;
  private static final String THREE_DOTS = "...";
  private static final String TWO_DOTS = "..";
  private static final String SEPARATOR = "/";
  @SuppressWarnings("UseJBColor") private static final JBColor BACKGROUND = new JBColor(Color.BLACK, Color.WHITE);
  private static final float BALANCE = 0.08f;
  private static final JBColor TEXT_COLOR = new JBColor(new Color(0x7a7a7a), new Color(0x909090));

  @NotNull private final VcsLogData myLogData;

  @NotNull private List<Pair<String, LabelIcon>> myLabels = ContainerUtil.newArrayList();
  private int myHeight = JBUI.scale(22);
  private int myWidth = 0;
  @NotNull private Color myBackground = UIUtil.getTableBackground();
  @Nullable private Color myGreyBackground = null;
  @NotNull private Color myForeground = UIUtil.getTableForeground();

  public LabelPainter(@NotNull VcsLogData data) {
    myLogData = data;
  }

  @Nullable
  public static VcsLogRefManager getRefManager(@NotNull VcsLogData logData, @NotNull Collection<VcsRef> references) {
    if (!references.isEmpty()) {
      VirtualFile root = ObjectUtils.assertNotNull(ContainerUtil.getFirstItem(references)).getRoot();
      return logData.getLogProvider(root).getReferenceManager();
    }
    else {
      return null;
    }
  }

  public void customizePainter(@NotNull JComponent component,
                               @NotNull Collection<VcsRef> references,
                               @NotNull Color background,
                               @NotNull Color foreground,
                               boolean isSelected,
                               int availableWidth) {
    myBackground = background;
    myForeground = isSelected ? foreground : TEXT_COLOR;

    FontMetrics metrics = component.getFontMetrics(getReferenceFont());
    myHeight = metrics.getHeight() + TOP_TEXT_PADDING + BOTTOM_TEXT_PADDING;

    VcsLogRefManager manager = getRefManager(myLogData, references);
    List<RefGroup> refGroups = manager == null ? ContainerUtil.emptyList() : manager.groupForTable(references);

    myGreyBackground = calculateGreyBackground(refGroups, background, isSelected);
    Pair<List<Pair<String, LabelIcon>>, Integer> presentation =
      calculatePresentation(refGroups, metrics, myHeight, myGreyBackground != null ? myGreyBackground : myBackground, availableWidth);

    myLabels = presentation.first;
    myWidth = presentation.second;
  }

  @NotNull
  private static Pair<List<Pair<String, LabelIcon>>, Integer> calculatePresentation(@NotNull List<RefGroup> refGroups,
                                                                                    @NotNull FontMetrics fontMetrics,
                                                                                    int height,
                                                                                    @NotNull Color background,
                                                                                    int availableWidth) {
    int width = LEFT_PADDING + RIGHT_PADDING;

    List<Pair<String, LabelIcon>> labels = ContainerUtil.newArrayList();
    if (refGroups.isEmpty()) return Pair.create(labels, width);

    for (RefGroup group : refGroups) {
      if (group.isExpanded()) {
        for (VcsRef ref : group.getRefs()) {
          LabelIcon labelIcon = new LabelIcon(height, background, ref.getType().getBackgroundColor());
          width += labelIcon.getIconWidth() + MIDDLE_PADDING;

          String text = shortenRefName(ref.getName(), fontMetrics, availableWidth - width);
          width += fontMetrics.stringWidth(text);

          labels.add(Pair.create(text, labelIcon));
        }
      }
      else {
        List<Color> colors = group.getColors();
        LabelIcon labelIcon = new LabelIcon(height, background, colors.toArray(new Color[colors.size()]));
        width += labelIcon.getIconWidth() + MIDDLE_PADDING;

        String text = shortenRefName(group.getName(), fontMetrics, availableWidth - width);
        width += fontMetrics.stringWidth(text);

        labels.add(Pair.create(text, labelIcon));
      }
    }

    return Pair.create(labels, width);
  }

  @Nullable
  private static Color calculateGreyBackground(@NotNull List<RefGroup> refGroups, @NotNull Color background, boolean isSelected) {
    if (isSelected) return null;

    boolean paintGreyBackground;
    for (RefGroup group : refGroups) {
      if (group.isExpanded()) {
        paintGreyBackground = ContainerUtil.find(group.getRefs(), ref -> !ref.getName().isEmpty()) != null;
      }
      else {
        paintGreyBackground = !group.getName().isEmpty();
      }

      if (paintGreyBackground) return ColorUtil.mix(background, BACKGROUND, BALANCE);
    }

    return null;
  }

  @NotNull
  private static String shortenRefName(@NotNull String refName, @NotNull FontMetrics fontMetrics, int availableWidth) {
    if (fontMetrics.stringWidth(refName) > availableWidth && refName.length() > MAX_LENGTH) {
      int separatorIndex = refName.indexOf(SEPARATOR);
      if (separatorIndex > TWO_DOTS.length()) {
        refName = TWO_DOTS + refName.substring(separatorIndex);
      }

      if (fontMetrics.stringWidth(refName) <= availableWidth) return refName;

      if (availableWidth > 0) {
        for (int i = refName.length(); i > MAX_LENGTH; i--) {
          String result = StringUtil.shortenTextWithEllipsis(refName, i, 0, THREE_DOTS);
          if (fontMetrics.stringWidth(result) <= availableWidth) {
            return result;
          }
        }
      }
      return StringUtil.shortenTextWithEllipsis(refName, MAX_LENGTH, 0, THREE_DOTS);
    }
    return refName;
  }

  public void paint(@NotNull Graphics2D g2, int x, int y, int height) {
    if (myLabels.isEmpty()) return;

    GraphicsConfig config = GraphicsUtil.setupAAPainting(g2);
    g2.setFont(getReferenceFont());
    g2.setStroke(new BasicStroke(1.5f));

    FontMetrics fontMetrics = g2.getFontMetrics();
    int baseLine = SimpleColoredComponent.getTextBaseLine(fontMetrics, height);

    g2.setColor(myBackground);
    g2.fillRect(x, y, myWidth, height);

    if (myGreyBackground != null) {
      g2.setColor(myGreyBackground);
      g2.fillRect(x, y + baseLine - fontMetrics.getAscent() - TOP_TEXT_PADDING,
                  myWidth - RIGHT_PADDING + LEFT_PADDING,
                  fontMetrics.getHeight() + TOP_TEXT_PADDING + BOTTOM_TEXT_PADDING);
    }

    x += LEFT_PADDING;

    for (Pair<String, LabelIcon> label : myLabels) {
      LabelIcon icon = label.second;
      String text = label.first;

      icon.paintIcon(null, g2, x, y + (height - icon.getIconHeight()) / 2);
      x += icon.getIconWidth();

      g2.setColor(myForeground);
      g2.drawString(text, x, y + baseLine);
      x += fontMetrics.stringWidth(text) + MIDDLE_PADDING;
    }

    config.restore();
  }

  public Dimension getSize() {
    if (myLabels.isEmpty()) return new Dimension();
    return new Dimension(myWidth, myHeight);
  }

  public boolean isLeftAligned() {
    return Registry.is("vcs.log.labels.left.aligned");
  }

  public Font getReferenceFont() {
    Font font = RectanglePainter.getFont();
    return font.deriveFont(font.getSize() - 1f);
  }
}

