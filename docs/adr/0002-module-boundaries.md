# ADR-0002: Ranh giới và phụ thuộc giữa các Module

**Status**: Proposed
**Date**: 2026-08-10
**Owners**: Tiến Luật

## Context

[ADR-0001: Sử dụng Modular Monolith cho Backend cốt lõi](0001-modular-monolith.md) quyết định chia Java Backend thành các module theo business capability. Tuy nhiên, việc chỉ tạo nhiều thư mục hoặc Maven/Gradle module chưa đủ để bảo vệ kiến trúc.

Nếu không có dependency rule rõ ràng, các vấn đề sau có thể xuất hiện:

- Strategy gọi trực tiếp Binance hoặc database;
- Backtester phụ thuộc vào từng implementation như MA hoặc RSI;
- Search gọi trực tiếp implementation của Backtest, Evaluation và Leaderboard;
- Controller chứa business logic hoặc truy cập repository;
- module khác truy cập trực tiếp bảng dữ liệu không thuộc quyền sở hữu;
- các module tạo dependency vòng, khiến thay đổi một phần ảnh hưởng toàn hệ thống;
- `contracts` hoặc một thư mục `utils` trở thành nơi chứa mọi loại logic dùng chung.

Đề bài yêu cầu có thể thêm Strategy, thay Search Algorithm, thay Market Data Provider và scale Backtest với ảnh hưởng tối thiểu. Vì vậy, ranh giới module phải được thể hiện bằng dependency direction, public contract và kiểm tra tự động, không chỉ bằng tài liệu.

## Decision

### 1. Phân loại module

Các module được chia thành bốn nhóm:

| Nhóm                | Module/Application                                                                                                        | Vai trò                                                      |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| Foundation          | `domain`, `contracts`                                                                                                     | Kiểu dữ liệu ổn định và contract đi qua boundary             |
| Business capability | `market-data`, `strategy-core`, `strategies`, `combination`, `backtesting`, `evaluation`, `search`, `leaderboard`, `news` | Chứa logic theo từng năng lực nghiệp vụ                      |
| Adapter             | `persistence`                                                                                                             | Kết nối PostgreSQL/Supabase, Redis và triển khai output port |
| Composition/runtime | `apps/api`, `apps/worker`                                                                                                 | Wiring module, transaction boundary và điều phối use case    |

`apps/web` và `apps/sentiment` là application boundary riêng. Chúng giao tiếp với Java Backend qua API/event contract, không tạo Java build dependency vào các module nội bộ.

### 2. Dependency direction

Dependency chỉ được hướng từ runtime/adapter vào public contract và domain ổn định:

```drawio
<mxfile>
  <diagram id="CpSP7lIE6N4JWnOPNXAj" name="Page-1">
    <mxGraphModel dx="2" dy="1" grid="0" gridSize="10" guides="1" tooltips="0" connect="0" arrows="0" fold="0" page="0" pageScale="1" pageWidth="850" pageHeight="1100" math="0" shadow="0">
      <root>
        <mxCell id="1tv6H6HBNWO-JgceilxE-0" />
        <mxCell id="1tv6H6HBNWO-JgceilxE-1" parent="1tv6H6HBNWO-JgceilxE-0" />
        <UserObject label="" mermaidData="{&#xa;  &quot;data&quot;: &quot;flowchart TD\n    WEB[apps/web] --&gt;|HTTP / WebSocket| API[apps/api]\n    SENTIMENT[apps/sentiment] &lt;--&gt;|HTTP contract| API\n\n    API --&gt; CAPABILITIES[Business Capability Modules]\n    WORKER[apps/worker] --&gt; CAPABILITIES\n    API --&gt; PERSISTENCE[persistence]\n    WORKER --&gt; PERSISTENCE\n\n    PERSISTENCE --&gt; PORTS[Capability Output Ports]\n    CAPABILITIES --&gt; CONTRACTS[contracts]\n    CAPABILITIES --&gt; DOMAIN[domain]\n    CONTRACTS --&gt; DOMAIN\n\n    STRATEGIES[strategies] --&gt; STRATEGY_CORE[strategy-core]\n    COMBINATION[combination] --&gt; STRATEGY_CORE\n    BACKTESTING[backtesting] --&gt; STRATEGY_CORE\n    SEARCH[search] --&gt; STRATEGY_CORE&quot;,&#xa;  &quot;config&quot;: null&#xa;}" id="_lOih-_n08xBNWANPp59-0">
          <mxCell connectable="0" parent="1tv6H6HBNWO-JgceilxE-1" style="group;transparentBounds=1;editIcon=1;lockedGroup=0;groupPadding=10;" vertex="1">
            <mxGeometry as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="apps/web" mermaidId="n:WEB" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="apps/web" id="1tv6H6HBNWO-JgceilxE-2">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="131" x="10" y="10" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="apps/api" mermaidId="n:API" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="apps/api" id="1tv6H6HBNWO-JgceilxE-3">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="123" x="115" y="133" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="apps/sentiment" mermaidId="n:SENTIMENT" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="apps/sentiment" id="1tv6H6HBNWO-JgceilxE-4">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="173" x="191" y="10" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="Business Capability Modules" mermaidId="n:CAPABILITIES" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="Business Capability Modules" id="1tv6H6HBNWO-JgceilxE-5">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="259" x="287" y="237" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="apps/worker" mermaidId="n:WORKER" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="apps/worker" id="1tv6H6HBNWO-JgceilxE-6">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="151" x="331" y="133" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="persistence" mermaidId="n:PERSISTENCE" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="persistence" id="1tv6H6HBNWO-JgceilxE-7">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="142" x="95" y="237" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="Capability Output Ports" mermaidId="n:PORTS" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="Capability Output Ports" id="1tv6H6HBNWO-JgceilxE-8">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="228" x="52" y="341" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="contracts" mermaidId="n:CONTRACTS" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="contracts" id="1tv6H6HBNWO-JgceilxE-9">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="127" x="402" y="341" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="domain" mermaidId="n:DOMAIN" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="domain" id="1tv6H6HBNWO-JgceilxE-10">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="112" x="361" y="445" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="strategies" mermaidId="n:STRATEGIES" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="strategies" id="1tv6H6HBNWO-JgceilxE-11">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="130" x="414" y="10" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="strategy-core" mermaidId="n:STRATEGY_CORE" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="strategy-core" id="1tv6H6HBNWO-JgceilxE-12">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="156" x="688" y="133" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="combination" mermaidId="n:COMBINATION" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="combination" id="1tv6H6HBNWO-JgceilxE-13">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="149" x="594" y="10" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="backtesting" mermaidId="n:BACKTESTING" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="backtesting" id="1tv6H6HBNWO-JgceilxE-14">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="143" x="793" y="10" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="search" mermaidId="n:SEARCH" mermaidBaseStyle="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" mermaidBaseValue="search" id="1tv6H6HBNWO-JgceilxE-15">
          <mxCell parent="_lOih-_n08xBNWANPp59-0" style="html=1;whiteSpace=wrap;strokeWidth=1;fillColor=light-dark(#ECECFF,#1f2020);strokeColor=light-dark(#9370DB,#cccccc);fontColor=light-dark(#333333,#cccccc);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontSize=16;" vertex="1">
            <mxGeometry height="54" width="106" x="986" y="10" as="geometry" />
          </mxCell>
        </UserObject>
        <UserObject label="HTTP / WebSocket" mermaidId="e:WEB-&gt;API#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);html=1;fontSize=16;labelBackgroundColor=light-dark(#E8E8E88D,#2a2a2a8D);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.14;entryY=0;" mermaidBaseValue="HTTP / WebSocket" id="1tv6H6HBNWO-JgceilxE-16">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-2" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);html=1;fontSize=16;labelBackgroundColor=light-dark(#E8E8E88D,#2a2a2a8D);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.14;entryY=0;" target="1tv6H6HBNWO-JgceilxE-3">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="75" y="99" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="HTTP contract" mermaidId="e:SENTIMENT-&gt;API#0" mermaidBaseStyle="curved=1;startArrow=block;startSize=7;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);html=1;fontSize=16;labelBackgroundColor=light-dark(#E8E8E88D,#2a2a2a8D);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.85;entryY=0;" mermaidBaseValue="HTTP contract" id="1tv6H6HBNWO-JgceilxE-17">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-4" style="curved=1;startArrow=block;startSize=7;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);html=1;fontSize=16;labelBackgroundColor=light-dark(#E8E8E88D,#2a2a2a8D);fontFamily=Trebuchet MS,Verdana,Arial,sans-serif;fontColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.85;entryY=0;" target="1tv6H6HBNWO-JgceilxE-3">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="277" y="99" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:API-&gt;CAPABILITIES#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=1;exitY=0.98;entryX=0.27;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-18">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-3" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=1;exitY=0.98;entryX=0.27;entryY=0;" target="1tv6H6HBNWO-JgceilxE-5">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="301" y="212" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:WORKER-&gt;CAPABILITIES#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.57;exitY=1;entryX=0.52;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-19">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-6" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.57;exitY=1;entryX=0.52;entryY=0;" target="1tv6H6HBNWO-JgceilxE-5">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="427" y="212" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:API-&gt;PERSISTENCE#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.41;exitY=1;entryX=0.46;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-20">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-3" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.41;exitY=1;entryX=0.46;entryY=0;" target="1tv6H6HBNWO-JgceilxE-7">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="156" y="212" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:WORKER-&gt;PERSISTENCE#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.02;exitY=1;entryX=0.87;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-21">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-6" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.02;exitY=1;entryX=0.87;entryY=0;" target="1tv6H6HBNWO-JgceilxE-7">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="266" y="212" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:PERSISTENCE-&gt;PORTS#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.5;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-22">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-7" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.5;entryY=0;" target="1tv6H6HBNWO-JgceilxE-8">
            <mxGeometry relative="1" as="geometry">
              <Array as="points" />
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:CAPABILITIES-&gt;CONTRACTS#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.6;exitY=1;entryX=0.5;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-23">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-5" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.6;exitY=1;entryX=0.5;entryY=0;" target="1tv6H6HBNWO-JgceilxE-9">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="466" y="316" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:CAPABILITIES-&gt;DOMAIN#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.4;exitY=1;entryX=0.26;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-24">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-5" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.4;exitY=1;entryX=0.26;entryY=0;" target="1tv6H6HBNWO-JgceilxE-10">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="366" y="316" />
                <mxPoint x="366" y="368" />
                <mxPoint x="366" y="420" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:CONTRACTS-&gt;DOMAIN#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.72;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-25">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-9" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.72;entryY=0;" target="1tv6H6HBNWO-JgceilxE-10">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="466" y="420" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:STRATEGIES-&gt;STRATEGY_CORE#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0;entryY=0.19;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-26">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-11" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0;entryY=0.19;" target="1tv6H6HBNWO-JgceilxE-12">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="479" y="99" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:COMBINATION-&gt;STRATEGY_CORE#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.22;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-27">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-13" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.22;entryY=0;" target="1tv6H6HBNWO-JgceilxE-12">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="668" y="99" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:BACKTESTING-&gt;STRATEGY_CORE#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.78;entryY=0;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-28">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-14" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=0.78;entryY=0;" target="1tv6H6HBNWO-JgceilxE-12">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="864" y="99" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
        <UserObject label="" mermaidId="e:SEARCH-&gt;STRATEGY_CORE#0" mermaidBaseStyle="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=1;entryY=0.17;" mermaidBaseValue="" id="1tv6H6HBNWO-JgceilxE-29">
          <mxCell edge="1" parent="_lOih-_n08xBNWANPp59-0" source="1tv6H6HBNWO-JgceilxE-15" style="curved=1;startArrow=none;endArrow=block;endSize=7;strokeColor=light-dark(#333333,#cccccc);exitX=0.5;exitY=1;entryX=1;entryY=0.17;" target="1tv6H6HBNWO-JgceilxE-12">
            <mxGeometry relative="1" as="geometry">
              <Array as="points">
                <mxPoint x="1039" y="99" />
              </Array>
            </mxGeometry>
          </mxCell>
        </UserObject>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>

```

Không module nào được tạo dependency ngược từ `domain` vào Spring, database, Binance hoặc một capability module.

### 3. Dependency được phép

| Module          | Được phụ thuộc trực tiếp                                                  |
| --------------- | ------------------------------------------------------------------------- |
| `domain`        | Không phụ thuộc module nội bộ nào                                         |
| `contracts`     | `domain`                                                                  |
| `market-data`   | `domain`, `contracts`                                                     |
| `strategy-core` | `domain`, `contracts`                                                     |
| `strategies`    | `domain`, `strategy-core`                                                 |
| `combination`   | `domain`, `strategy-core`                                                 |
| `backtesting`   | `domain`, `contracts`, `strategy-core`                                    |
| `evaluation`    | `domain`, `contracts`                                                     |
| `search`        | `domain`, `contracts`, `strategy-core`                                    |
| `leaderboard`   | `domain`, `contracts`                                                     |
| `news`          | `domain`, `contracts`                                                     |
| `persistence`   | `domain`, `contracts` và output port công khai của module sở hữu dữ liệu  |
| `apps/api`      | Public API của các capability module, `contracts`, `persistence`          |
| `apps/worker`   | Public API cần thiết cho Backtest/Search flow, `contracts`, `persistence` |

Thêm dependency ngoài bảng phải cập nhật ADR này hoặc tạo ADR thay thế và được nhóm review.

### 4. Dependency bị cấm

| From                          | Không được phụ thuộc                                                 | Lý do                                                         |
| ----------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------- |
| `domain`                      | Spring, database driver, Binance SDK, module nghiệp vụ               | Giữ domain độc lập và dễ kiểm thử                             |
| `strategy-core`, `strategies` | `persistence`, `market-data`, Spring Web, `apps/*`                   | Strategy chỉ phân tích input và trả BUY/SELL/HOLD             |
| `backtesting`                 | Strategy implementation cụ thể, `search`, `leaderboard`, Controller  | Backtester chỉ làm việc với Strategy contract                 |
| `evaluation`                  | Strategy implementation, `search`, `leaderboard`, database adapter   | Evaluation phải thay đổi độc lập với Strategy và Ranking      |
| `search`                      | Implementation cụ thể của `backtesting`, `evaluation`, `leaderboard` | Có thể thay Search Algorithm mà không sửa các module phía sau |
| `leaderboard`                 | `search` hoặc `backtesting` implementation                           | Leaderboard chỉ nhận Evaluation Result chuẩn hóa              |
| `news`                        | Sentiment model implementation                                       | Có thể thay crawler/provider và model độc lập                 |
| Capability module             | Repository/table nội bộ của module khác                              | Bảo vệ data ownership                                         |
| `apps/web`                    | Binance hoặc Java module nội bộ                                      | Frontend chỉ phụ thuộc API/WebSocket contract chuẩn hóa       |

### 5. Quy tắc public contract

Mỗi capability module chỉ công khai những thành phần cần thiết qua các package sau:

```text
<module>/api/       Public use case và facade
<module>/port/in/   Input port
<module>/port/out/  Output port cần adapter triển khai
<module>/event/     Event được phép phát ra ngoài module
```

Implementation, entity nội bộ và helper được đặt trong package `internal` hoặc package không được module khác import trực tiếp.

Quy tắc sử dụng:

1. Module khác gọi public use case/facade, không gọi class `internal`.
2. DTO qua HTTP, WebSocket, queue hoặc service boundary nằm trong `contracts` khi thực sự được nhiều runtime dùng chung.
3. Model chỉ dùng bên trong một module phải ở lại module đó, không chuyển vào `contracts` để tiện import.
4. Không tạo `common`, `shared` hoặc `utils` chứa business logic chung chung.
5. Business logic không nằm trong Controller, Spring configuration, repository adapter hoặc mapper.

### 6. Quy tắc điều phối luồng

`apps/api` hoặc `apps/worker` được phép điều phối nhiều public use case để hoàn thành một application flow. Ví dụ Search loop có thể thực hiện:

```text
Generate Candidate
  -> Backtest
  -> Evaluate
  -> Rank
  -> Update Top-K
```

Tuy nhiên:

- `search` chỉ chịu trách nhiệm sinh candidate và stop condition;
- `backtesting` chỉ mô phỏng giao dịch;
- `evaluation` chỉ tính metrics;
- `leaderboard` chỉ tính ranking và duy trì Top-K;
- orchestration không được sao chép business rule của các module trên.

Khi chuyển sang Queue/Worker, contract và boundary vẫn giữ nguyên theo [ADR-0006: Queue và Worker cho Backtest/Search](0006-queue-worker-backtesting.md).

### 7. Quy tắc adapter và dữ liệu

- Binance là adapter phía ngoài của `market-data`, theo [ADR-0003: Market Data Adapter](0003-market-data-adapter.md).
- Strategy được đăng ký qua contract/registry, theo [ADR-0005: Strategy Plugin Registry](0005-strategy-plugin-registry.md).
- `persistence` triển khai output port do module sở hữu khai báo; capability module không import repository implementation.
- Quyền sở hữu PostgreSQL/Supabase và Redis được quy định trong [ADR-0007: PostgreSQL và Redis Ownership](0007-postgresql-redis-ownership.md).
- Sentiment Service chỉ được truy cập qua contract của [ADR-0008: Tách Sentiment Service](0008-sentiment-service-boundary.md).
- Experiment, Strategy Version và Dataset Version tuân theo [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md).

## Alternatives Considered

- **Cho phép module import lẫn nhau tự do**: Nhanh trong vài ngày đầu nhưng tạo dependency vòng và làm mất khả năng thay thế Strategy, Search hoặc Provider.
- **Chia theo technical layer toàn cục (`controller/service/repository`)**: Dễ hiểu với ứng dụng nhỏ nhưng các file của một capability bị phân tán, boundary nghiệp vụ không rõ.
- **Mọi giao tiếp đều qua event**: Giảm coupling trực tiếp nhưng làm luồng MVP khó debug, tăng eventual consistency và không cần thiết cho mọi use case.
- **Đưa tất cả model vào `contracts` hoặc `shared`**: Giảm lỗi import trước mắt nhưng tạo shared kernel quá lớn; thay đổi một model có thể ảnh hưởng toàn hệ thống.
- **Mỗi module có database/service riêng ngay từ đầu**: Boundary mạnh nhưng tăng chi phí triển khai và vận hành vượt nhu cầu của nhóm bốn người.

## Consequences

### Positive

- Có thể nhìn vào dependency graph để hiểu module nào được phép gọi module nào.
- Strategy, Search, Evaluation và Leaderboard có trách nhiệm tách biệt.
- Thêm MACD không yêu cầu sửa Backtester hoặc Evaluation.
- Có thể thay Binance Adapter mà không đổi contract của Frontend.
- Có nền tảng để tách Worker hoặc service riêng trong tương lai.
- Architecture violation có thể được phát hiện tự động trước khi merge.

### Negative

- Cần thêm interface, DTO, mapper và wiring ở boundary.
- Một số luồng đơn giản có nhiều bước hơn so với gọi thẳng repository/service.
- Thành viên phải hiểu public API và data ownership trước khi code.
- Dependency matrix phải được cập nhật khi xuất hiện module hoặc use case mới.

## Affected Components

- `apps/api`
- `apps/worker`
- `apps/web`
- `apps/sentiment`
- toàn bộ thư mục `modules/`
- architecture tests trong Java test suite
- Pull Request checklist và code review

## Validation

- Tạo ArchUnit test cấm `domain` phụ thuộc Spring, persistence hoặc adapter package.
- Tạo ArchUnit test cấm `strategies` và `strategy-core` phụ thuộc `persistence` hoặc `market-data`.
- Tạo test phát hiện dependency cycle giữa các capability module.
- Build tool phải khai báo dependency đúng với bảng “Dependency được phép”.
- Thêm Strategy giả `MACDStrategy` và xác nhận không sửa Backtester/Evaluator.
- Thêm Search Generator giả và xác nhận không sửa Backtester/Leaderboard.
- Thay Binance Adapter bằng fixture adapter và xác nhận API response không đổi.
- Review Pull Request phải từ chối import trực tiếp package `internal` của module khác.

## Risks and Mitigations

- **Risk**: Quy tắc quá chặt làm chậm tiến độ MVP.

  **Mitigation**: Chỉ bảo vệ boundary ảnh hưởng architectural driver; cho phép orchestration ở application layer thay vì tạo abstraction cho mọi hàm.

- **Risk**: `contracts` phát triển thành shared dumping ground.

  **Mitigation**: Mọi contract mới phải có ít nhất hai runtime/module thực sự sử dụng hoặc đi qua external boundary.

- **Risk**: `persistence` phụ thuộc quá nhiều module và trở thành điểm coupling.

  **Mitigation**: Chia adapter theo package của owner; mỗi adapter chỉ triển khai output port tương ứng, không chứa business flow.

- **Risk**: Thành viên bỏ qua boundary để sửa nhanh.

  **Mitigation**: Enforce bằng build dependency, ArchUnit và PR review thay vì chỉ dựa vào tài liệu.

- **Risk**: Orchestration trong `apps/api` trở thành God Service mới.

  **Mitigation**: Mỗi application flow có use-case coordinator nhỏ; business decision phải nằm trong module sở hữu.

## References

- [Đề bài Crypto StrategyLab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Architecture Overview](../architecture/architecture-overview.md)
- [Module View](../architecture/module-view.md)
- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0003: Market Data Adapter](0003-market-data-adapter.md)
- [ADR-0005: Strategy Plugin Registry](0005-strategy-plugin-registry.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)
- [ADR-0007: PostgreSQL và Redis Ownership](0007-postgresql-redis-ownership.md)
- [ADR-0008: Sentiment Service Boundary](0008-sentiment-service-boundary.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)

## Supersession

- Supersedes: None
- Superseded by: None
