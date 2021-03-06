/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
function bindUpload(markupId, hiddenId) {
	var fi = $('#' + markupId + ' .fileinput');
	if (!fi.eventAdded) {
		$('#' + markupId + ' .fileinput').on('change.bs.fileinput', function(event) {
			event.stopPropagation();
			var th = $(this),
			fInput = th.find('input[type=file]'),
			fn = th.find('.fileinput-filename');
			if (fInput[0].files !== undefined && fInput[0].files.length > 1) {
				fn.text($.map(fInput[0].files, function(val) { return val.name; }).join(', '));
			}
			fInput.attr('title', fn.text());
			var hi = $('#' + hiddenId);
			hi.val(fn.text());
			hi.trigger('change');
			return false;
		});
		fi.eventAdded = true;
	}
}
