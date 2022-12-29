/*! For license information please see createPlaneOutlineGeometry.js.LICENSE.txt */
define(["./defaultValue-135942ca","./Transforms-3ea76111","./Matrix3-edb29a7e","./ComponentDatatype-e86a9f87","./GeometryAttribute-dacddb3f","./GeometryAttributes-899f8bd0","./Math-a304e2d6","./Matrix2-7a2bab7e","./RuntimeError-f0dada00","./combine-462d91dd","./WebGLConstants-fcb70ee3"],(function(e,t,n,r,a,i,o,u,c,d,s){"use strict";function y(){this._workerName="createPlaneOutlineGeometry"}y.packedLength=0,y.pack=function(e,t){return t},y.unpack=function(t,n,r){return e.defined(r)?r:new y};const m=new n.Cartesian3(-.5,-.5,0),p=new n.Cartesian3(.5,.5,0);return y.createGeometry=function(){const e=new i.GeometryAttributes,o=new Uint16Array(8),u=new Float64Array(12);return u[0]=m.x,u[1]=m.y,u[2]=m.z,u[3]=p.x,u[4]=m.y,u[5]=m.z,u[6]=p.x,u[7]=p.y,u[8]=m.z,u[9]=m.x,u[10]=p.y,u[11]=m.z,e.position=new a.GeometryAttribute({componentDatatype:r.ComponentDatatype.DOUBLE,componentsPerAttribute:3,values:u}),o[0]=0,o[1]=1,o[2]=1,o[3]=2,o[4]=2,o[5]=3,o[6]=3,o[7]=0,new a.Geometry({attributes:e,indices:o,primitiveType:a.PrimitiveType.LINES,boundingSphere:new t.BoundingSphere(n.Cartesian3.ZERO,Math.sqrt(2))})},function(t,n){return e.defined(n)&&(t=y.unpack(t,n)),y.createGeometry(t)}}));