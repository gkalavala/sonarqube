/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockIssue } from '../../../../helpers/testMocks';
import { click, waitAndUpdate } from '../../../../helpers/testUtils';
import ConciseIssueBox from '../ConciseIssueBox';

it('should render correctly', async () => {
  const onClick = jest.fn();
  const issue = mockIssue();
  const wrapper = shallowRender({ onClick, issue });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.concise-issue-box'));
  expect(onClick).toHaveBeenCalledWith(issue.key);

  expect(shallowRender({ issue: mockIssue(true), selected: true })).toMatchSnapshot();
});

const shallowRender = (props: Partial<ConciseIssueBox['props']> = {}) => {
  return shallow(
    <ConciseIssueBox
      issue={mockIssue()}
      onClick={jest.fn()}
      onFlowSelect={jest.fn()}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selected={false}
      selectedFlowIndex={0}
      selectedLocationIndex={0}
      {...props}
    />
  );
};
