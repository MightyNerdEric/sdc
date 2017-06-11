/*!
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import {actionTypes, forms} from './SoftwareProductComponentsConstants.js';

export default (state = {}, action) => {
	switch (action.type) {
		case actionTypes.COMPONENT_LOAD:
			return {
				...state,
				data: action.component,
				formReady: null,
				formName: forms.ALL_SPC_FORMS,
				genericFieldInfo: {
					'displayName' : {
						isValid: true,
						errorText: '',
						validations: []
					},
					'vfcCode' : {
						isValid: true,
						errorText: '',
						validations: []
					},
					'description' : {
						isValid: true,
						errorText: '',
						validations: []
					}
				}
			};
		default:
			return state;
	}
};
