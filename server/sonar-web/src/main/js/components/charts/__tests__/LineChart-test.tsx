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
import LineChart from '../LineChart';

it('should display line', () => {
  const data = [
    { x: 1, y: 10 },
    { x: 2, y: 30 },
    { x: 3, y: 20 },
  ];
  const chart = shallow(<LineChart data={data} height={100} width={100} />);
  expect(chart.find('.line-chart-path').length).toBe(1);
});

it('should display ticks', () => {
  const data = [
    { x: 1, y: 10 },
    { x: 2, y: 30 },
    { x: 3, y: 20 },
  ];
  const ticks = ['A', 'B', 'C'];
  const chart = shallow(<LineChart data={data} height={100} width={100} xTicks={ticks} />);
  expect(chart.find('.line-chart-tick').length).toBe(3);
});

it('should display values', () => {
  const data = [
    { x: 1, y: 10 },
    { x: 2, y: 30 },
    { x: 3, y: 20 },
  ];
  const values = ['A', 'B', 'C'];
  const chart = shallow(<LineChart data={data} height={100} width={100} xValues={values} />);
  expect(chart.find('.line-chart-tick').length).toBe(3);
});
