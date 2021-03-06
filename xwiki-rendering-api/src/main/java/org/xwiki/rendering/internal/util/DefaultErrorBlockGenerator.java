/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.util.ErrorBlockGenerator;

/**
 * Default implementation to generate error blocks to render an error in a wiki page.
 *
 * @version $Id$
 * @since 8.1M1
 */
@Component
@Singleton
public class DefaultErrorBlockGenerator implements ErrorBlockGenerator
{
    @Inject
    private Logger logger;

    @Override
    public List<Block> generateErrorBlocks(String message, String description, boolean isInline)
    {
        List<Block> errorBlocks = new ArrayList<>();

        Map<String, String> errorBlockParams =
            Collections.singletonMap(CLASS_ATTRIBUTE_NAME, CLASS_ATTRIBUTE_MESSAGE_VALUE);
        Map<String, String> errorDescriptionBlockParams =
            Collections.singletonMap(CLASS_ATTRIBUTE_NAME, CLASS_ATTRIBUTE_DESCRIPTION_VALUE);

        Block descriptionBlock = new VerbatimBlock(description, isInline);

        if (isInline) {
            errorBlocks.add(new FormatBlock(Arrays.asList(new WordBlock(message)), Format.NONE, errorBlockParams));
            errorBlocks.add(new FormatBlock(Arrays.asList(descriptionBlock), Format.NONE, errorDescriptionBlockParams));
        } else {
            errorBlocks.add(new GroupBlock(Arrays.asList(new WordBlock(message)), errorBlockParams));
            errorBlocks.add(new GroupBlock(Arrays.asList(descriptionBlock), errorDescriptionBlockParams));
        }

        return errorBlocks;
    }

    @Override
    public List<Block> generateErrorBlocks(String messagePrefix, Throwable throwable, boolean isInline)
    {
        // Note: We're using ExceptionUtils.getRootCause(e).getMessage() instead of getRootCauseMessage()
        //       below because getRootCauseMessage() adds a technical prefix (the name of the exception), that
        //       we don't want to display to our users.
        Throwable rootCause = ExceptionUtils.getRootCause(throwable);
        if (rootCause == null) {
            // If there's no nested exception, fall back to the throwable itself for getting the cause
            rootCause = throwable;
        }

        String augmentedMessage = String.format("%s%s", messagePrefix,
            rootCause == null ? "" : String.format(". Cause: [%s]", rootCause.getMessage()));
        augmentedMessage = String.format("%s%sClick on this message for details.", augmentedMessage,
            augmentedMessage.trim().endsWith(".") ? " " : ". ");

        this.logger.debug(augmentedMessage);

        return generateErrorBlocks(augmentedMessage, ExceptionUtils.getStackTrace(throwable), isInline);
    }
}
